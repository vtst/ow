package net.vtst.ow.closure.compiler.deps;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.vtst.ow.closure.compiler.magic.MagicDepsGenerator;
import net.vtst.ow.closure.compiler.util.CompilerUtils;
import net.vtst.ow.closure.compiler.util.FileTreeVisitor;
import net.vtst.ow.closure.compiler.util.FileUtils;

import com.google.common.collect.Sets;
import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.deps.DependencyInfo;
import com.google.javascript.jscomp.deps.DepsFileParser;
import com.google.javascript.jscomp.deps.SortedDependencies.CircularDependencyException;

/**
 * A compilation set which represents a frozen JavaScript library.  The set of files and the
 * dependencies are stored in a deps.js file, so that it is not necessary to recompute it
 * at every loading.
 * <br>
 * <b>Thread safety:</b>  This class is thread safe one initialized.
 * @author Vincent Simonet
 */
public class JSLibrary extends AbstractJSProject {
  
  public enum StripMode {
    DISABLED,
    READ_ONLY,
    READ_AND_WRITE
  }
  
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

  private File path;
  private File pathOfClosureBase;
  private File depsFile;
  private boolean canWriteDepsFile = false;
  private boolean isClosureBase;
  private StripMode stripMode = StripMode.DISABLED;
  
  /**
   * Create a new library.
   * @param path  The root directory for the library.
   */
  public JSLibrary(File path) {
    this(path, path, true);
  }
  
  public JSLibrary(File path, File pathOfClosureBase, boolean isClosureBase) {
    System.out.println("Creating library for: " + path.getAbsolutePath());
    this.path = path;
    this.pathOfClosureBase = pathOfClosureBase;
    this.isClosureBase = isClosureBase;
  }
  
  public JSLibrary(File path, File pathOfClosureBase, boolean isClosureBase, StripMode stripMode) {
    this(path, pathOfClosureBase, isClosureBase);
    this.stripMode = stripMode;
  }

  
  @Override
  protected List<AbstractJSProject> getReferencedProjects() {
    return Collections.emptyList();
  }

  /**
   * Initialize the set of units of the library, either by reading the deps.js file,
   * or by browsing the source tree.
   * @param compiler
   */
  public void setUnits(AbstractCompiler compiler) {
    try {
      setUnits(compiler, getUnits(compiler));
    } catch (CircularDependencyException e) {
      reportError(compiler, e);
    }
  }
  
  /**
   * Find the dependency file (deps.js or deps.eclipse.js in the root directory).
   * @return true if the file has been found, false otherwise.
   */
  private boolean findDepsFile() {
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
  
  /**
   * Get the units for the library, either by reading the deps.js file, or by visiting the
   * file tree.
   * @param compiler  The compiler used to report errors.
   * @return  The list of units.
   */
  private List<JSUnit> getUnits(AbstractCompiler compiler) {
    if (findDepsFile()) {
      return readDepsFile(compiler, depsFile);
    } else {
      List<JSUnit> units = getUnitsByVisitingFiles(compiler);
      if (canWriteDepsFile) writeDepsFile(compiler, depsFile, units);
      return units;
    }
  }
  
  // **************************************************************************
  // Update from file tree

  /**
   * Update the set of compilation units and the dependencies from the file system.
   * @param compiler  The compiler to use to report errors.
   */
  private List<JSUnit> getUnitsByVisitingFiles(final AbstractCompiler compiler) {
    final List<JSUnit> units = new ArrayList<JSUnit>();
    FileTreeVisitor.Simple<RuntimeException> visitor = new FileTreeVisitor.Simple<RuntimeException>() {
      public void visitFile(java.io.File file) {
        if (!CompilerUtils.isJavaScriptFile(file)) return;
        JSUnit unit = new JSUnit(file, pathOfClosureBase, new JSUnitProvider.FromLibraryFile(file, stripMode));
        unit.updateDependencies(compiler);
        addGoogToDependencies(unit);
        units.add(unit);
      }
    };
    visitor.visit(path);
    return units;
  }
  
  /**
   * Dependencies to 'goog' (defined in base.js) are not correctly specified in the files of the
   * Closure Library.  This is to patch this.
   * @param requires
   */
  private void addGoogToDependencies(JSUnit unit) {
    if (!isClosureBase) return;
    if (unit.getPathRelativeToClosureBase().equals(BASE_FILE)) {
      unit.addProvide(GOOG);
    } else {
      unit.addRequire(GOOG);     
    }
  }

  // **************************************************************************
  // Reading and writing deps.js files
  
  /**
   * Reads a deps.js file.
   * @param compiler  The compiler to use for error reporting.
   * @param depsFile  The path to the deps.js file.
   */
  private List<JSUnit> readDepsFile(AbstractCompiler compiler, File depsFile) {
    List<JSUnit> units = new ArrayList<JSUnit>();
    try {
      DepsFileParser depsFileParser = new DepsFileParser(compiler.getErrorManager());
      for (DependencyInfo info: depsFileParser.parseFile(depsFile.getAbsolutePath())) {
        File file = FileUtils.join(pathOfClosureBase, new File(info.getPathRelativeToClosureBase()));
        JSUnit unit = new JSUnit(
            file, pathOfClosureBase, new JSUnitProvider.FromLibraryFile(file, stripMode), 
            Sets.newHashSet(info.getProvides()), Sets.newHashSet(info.getRequires()));
        addGoogToDependencies(unit);
        units.add(unit);
      }
    } catch (IOException exn) {
      CompilerUtils.reportError(compiler, JSError.make(OW_IO_ERROR, exn.getMessage()));            
    }
    return units;
  }
  
  /**
   * Write a deps.js file.
   * @param compiler  The compiler to use for error reporting.
   * @param depsFile  The path to the deps.js file.
   */
  private void writeDepsFile(AbstractCompiler compiler, File file, List<JSUnit> units) {
    try {
      MagicDepsGenerator depsGenerator = new MagicDepsGenerator();
      PrintStream out = new PrintStream(file);
      depsGenerator.writeDepInfos(out, units);
      out.close();
    } catch (IOException exn) {
      CompilerUtils.reportError(compiler, JSError.make(OW_IO_ERROR, exn.getMessage()));            
    }
  }
}
