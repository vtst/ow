package net.vtst.ow.closure.compiler.deps;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import net.vtst.ow.closure.compiler.magic.MagicDepsGenerator;
import net.vtst.ow.closure.compiler.util.CompilerUtils;
import net.vtst.ow.closure.compiler.util.FileTreeVisitor;
import net.vtst.ow.closure.compiler.util.FileUtils;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.deps.DependencyInfo;
import com.google.javascript.jscomp.deps.DepsFileParser;

/**
 * A compilation set which represents a frozen JavaScript library.  The set of files and the
 * dependencies are stored in a deps.js file, so that it is not necessary to recompute it
 * at every loading.
 * @author Vincent Simonet
 */
public class JSLibrary implements IJSSet {
  
  // TODO: It should be checked whether this works on Microsoft Windows, because the paths
  // in the deps.js file are stored with '/' instead of '\'.
  
  public static final String GOOG = "goog";
  private static final String BASE_FILE = "base.js";
  private static final String LEGACY_DEPS_FILE = "deps.js";
  private static final String GENERATED_DEPS_FILE = "deps.ow.js";
  
  static final DiagnosticType OW_DUPLICATED_GOOG_PROVIDE = DiagnosticType.warning(
      "OW_DUPLICATED_GOOG_PROVIDE",
      "The package \"{0}\" is already provided by \"{1}\"");

  static final DiagnosticType OW_IO_ERROR = DiagnosticType.warning(
      "OW_IO_ERROR",
      "I/O error: {0}");
  
  static final DiagnosticType OW_CANNOT_PARSE_DEPS_LINE = DiagnosticType.warning(
      "OW_CANNOT_PARSE_DEPS_LINE",
      "Cannot parse line.");

  private HashMap<String, JSUnit> providedBy = new HashMap<String, JSUnit>();
  private Collection<JSUnit> compilationUnits = new HashSet<JSUnit>();
  private File path;
  private File pathOfClosureBase;
  private File depsFile;
  private boolean canWriteDepsFile = false;
  private boolean isInitialized = false;
  private boolean isClosureBase;
  
  /**
   * Create a new library.
   * @param path  The root directory for the library.
   */
  public JSLibrary(File path) {
    this(path, path, true);
  }
  
  public JSLibrary(File path, File pathOfClosureBase, boolean isClosureBase) {
    this.path = path;
    this.pathOfClosureBase = pathOfClosureBase;
    this.isClosureBase = isClosureBase;
  }

  /* (non-Javadoc)
   * @see net.vtst.ow.closure.compiler.compile.ICompilationSet#getProvider(java.lang.String)
   */
  @Override
  public JSUnit getProvider(String name) {
    return providedBy.get(name);
  }

  /* (non-Javadoc)
   * @see net.vtst.ow.closure.compiler.compile.ICompilationSet#updateDependencies(com.google.javascript.jscomp.AbstractCompiler)
   */
  @Override
  public boolean updateDependencies(AbstractCompiler compiler) {
    if (isInitialized) return false;
    if (findDepsFile()) {
      readDepsFile(compiler, depsFile);
    } else {
      updateFromFileTree(compiler);
    }
    isInitialized = true;
    return true;
  }
  
  /**
   * Find the dependency file (deps.js or deps.eclipse.js in the root directory).
   * @return true if the file has been found, false otherwise.
   */
  private boolean findDepsFile() {
    if (depsFile != null) return depsFile.exists();
    depsFile = new File(path, LEGACY_DEPS_FILE);
    if (depsFile.exists()) {
      canWriteDepsFile = false;
      return true;
    } else {
      depsFile = new File(path, GENERATED_DEPS_FILE);
      canWriteDepsFile = true;
      return depsFile.exists();
    }
  }
  
  // **************************************************************************
  // Update from file tree

  /**
   * Update the set of compilation units and the dependencies from the file system.
   * @param compiler  The compiler to use to report errors.
   */
  public void updateFromFileTree(final AbstractCompiler compiler) {
    findDepsFile();
    compilationUnits.clear();
    providedBy.clear();
    FileTreeVisitor.Simple<RuntimeException> visitor = new FileTreeVisitor.Simple<RuntimeException>() {
      public void visitFile(java.io.File file) {
        if (!CompilerUtils.isJavaScriptFile(file)) return;
        JSUnit compilationUnit = new JSUnit(
            file, 
            pathOfClosureBase,
            new JSUnitProvider.FromFile(file));
        compilationUnit.updateDependencies(compiler);
        addGoogToDependencies(compilationUnit);
        addCompilationUnit(compiler, compilationUnit);
      }
    };
    visitor.visit(path);
    if (canWriteDepsFile) writeDepsFile(compiler, depsFile);
  }
  
  /**
   * Dependencies to 'goog' (defined in base.js) are not correctly specified in the files of the
   * Closure Library.  This is to patch this.
   * @param requires
   */
  private void addGoogToDependencies(JSUnit unit) {
    if (!isClosureBase) return;
    if (unit.getPathRelativeToClosureBase().equals(BASE_FILE)) {
      unit.getProvides().add(GOOG);
    } else {
      String prefix = GOOG + ".";
      Collection<String> requires = unit.getRequires();
      for (String require: requires) {
        if (require.startsWith(prefix)) return;
      }
      requires.add(GOOG);      
    }
  }

  /**
   * Add a compilation unit to the library.
   * @param compiler  The compiler used to report errors.
   * @param compilationUnit  The compilation unit to add.
   */
  private void addCompilationUnit(AbstractCompiler compiler, JSUnit compilationUnit) {
    compilationUnits.add(compilationUnit);
    for (String providedName: compilationUnit.getProvides()) {
      JSUnit previousCompilationUnit = providedBy.put(providedName, compilationUnit);
      if (previousCompilationUnit != null) {
        CompilerUtils.reportError(
            compiler, 
            JSError.make(previousCompilationUnit.getName(), 0, 0, OW_DUPLICATED_GOOG_PROVIDE, providedName, compilationUnit.getName()));            
      }
    }
  }

  // **************************************************************************
  // Reading and writing deps.js files
  
  /**
   * Reads a deps.js file.
   * @param compiler  The compiler to use for error reporting.
   * @param depsFile  The path to the deps.js file.
   */
  private void readDepsFile(AbstractCompiler compiler, File depsFile) {
    try {
      DepsFileParser depsFileParser = new DepsFileParser(compiler.getErrorManager());
      for (DependencyInfo info: depsFileParser.parseFile(depsFile.getAbsolutePath())) {
        File file = FileUtils.join(pathOfClosureBase, new File(info.getPathRelativeToClosureBase()));
        JSUnit compilationUnit = 
            new JSUnit(file, pathOfClosureBase, new JSUnitProvider.FromFile(file));
        compilationUnit.setDependencies(info.getProvides(), info.getRequires());
        addGoogToDependencies(compilationUnit);
        addCompilationUnit(compiler, compilationUnit);
      }
    } catch (IOException exn) {
      CompilerUtils.reportError(compiler, JSError.make(OW_IO_ERROR, exn.getMessage()));            
    }
  }
  
  /**
   * Write a deps.js file.
   * @param compiler  The compiler to use for error reporting.
   * @param depsFile  The path to the deps.js file.
   */
  private void writeDepsFile(AbstractCompiler compiler, File file) {
    try {
      MagicDepsGenerator depsGenerator = new MagicDepsGenerator();
      PrintStream out = new PrintStream(file);
      depsGenerator.writeDepInfos(out, compilationUnits);
      out.close();
    } catch (IOException exn) {
      CompilerUtils.reportError(compiler, JSError.make(OW_IO_ERROR, exn.getMessage()));            
    }
  }

}
