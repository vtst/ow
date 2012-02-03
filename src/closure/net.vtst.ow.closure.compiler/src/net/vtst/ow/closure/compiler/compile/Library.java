package net.vtst.ow.closure.compiler.compile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import net.vtst.ow.closure.compiler.compile.DepsFileTokenizer.Token;
import net.vtst.ow.closure.compiler.util.FileTreeVisitor;
import net.vtst.ow.closure.compiler.util.FileUtils;
import net.vtst.ow.closure.compiler.util.StringEscapeUtils;
import net.vtst.ow.closure.compiler.util.Utils;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;

/**
 * A compilation set which represents a frozen JavaScript library.  The set of files and the
 * dependencies are stored in a deps.js file, so that it is not necessary to recompute it
 * at every loading.
 * @author Vincent Simonet
 */
public class Library implements ICompilationSet {
  
  // TODO: It should be checked whether this works on Microsoft Windows, because the paths
  // in the deps.js file are stored with '/' instead of '\'.
  
  static final String LEGACY_DEPS_FILE = "deps.js";
  static final String GENERATED_DEPS_FILE = "deps.ow.js";
  
  static final DiagnosticType OW_DUPLICATED_GOOG_PROVIDE = DiagnosticType.warning(
      "OW_DUPLICATED_GOOG_PROVIDE",
      "The package \"{0}\" is already provided by \"{1}\"");

  static final DiagnosticType OW_IO_ERROR = DiagnosticType.warning(
      "OW_IO_ERROR",
      "I/O error: {0}");
  
  static final DiagnosticType OW_CANNOT_PARSE_DEPS_LINE = DiagnosticType.warning(
      "OW_CANNOT_PARSE_DEPS_LINE",
      "Cannot parse line.");

  private HashMap<String, CompilationUnit> providedBy = new HashMap<String, CompilationUnit>();
  private Collection<CompilationUnit> compilationUnits = new HashSet<CompilationUnit>();
  private File rootDir;
  private File referenceDirForDepsFile;
  private File depsFile;
  private boolean canWriteDepsFile = false;
  private boolean isInitialized = false;
  
  /**
   * Create a new library.
   * @param rootDir  The root directory for the library.
   */
  public Library(File rootDir) {
    this(rootDir, rootDir);
  }
  
  public Library(File rootDir, File referenceDirForDepsFile) {
    this.rootDir = rootDir;
    this.referenceDirForDepsFile = referenceDirForDepsFile;
  }

  /* (non-Javadoc)
   * @see net.vtst.ow.closure.compiler.compile.ICompilationSet#getProvider(java.lang.String)
   */
  @Override
  public CompilationUnit getProvider(String name) {
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
    depsFile = new File(rootDir, LEGACY_DEPS_FILE);
    if (depsFile.exists()) {
      canWriteDepsFile = false;
      return true;
    } else {
      depsFile = new File(rootDir, GENERATED_DEPS_FILE);
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
        if (!Utils.isJavaScriptFile(file)) return;
        CompilationUnit compilationUnit = new CompilationUnit(
            file.getPath(), 
            new CompilationUnitProvider.FromFile(file));
        compilationUnit.updateDependencies(compiler);
        addCompilationUnit(compiler, compilationUnit);
      }
    };
    visitor.visit(rootDir);
    if (canWriteDepsFile) writeDepsFile(compiler, depsFile);
  }

  /**
   * Add a compilation unit to the library.
   * @param compiler  The compiler used to report errors.
   * @param compilationUnit  The compilation unit to add.
   */
  private void addCompilationUnit(AbstractCompiler compiler, CompilationUnit compilationUnit) {
    compilationUnits.add(compilationUnit);
    for (String providedName: compilationUnit.getProvidedNames()) {
      CompilationUnit previousCompilationUnit = providedBy.put(providedName, compilationUnit);
      if (previousCompilationUnit != null) {
        Utils.reportError(
            compiler, 
            JSError.make(previousCompilationUnit.getName(), 0, 0, OW_DUPLICATED_GOOG_PROVIDE, providedName, compilationUnit.getName()));            
      }
    }
  }

  // **************************************************************************
  // Parsing of deps.js files
  
  private Collection<String> parseListUntil(DepsFileTokenizer tokenizer, int close) throws IOException {
    Collection<String> result = new ArrayList<String>();
    while (true) {
      Token token = tokenizer.nextToken();
      if (token == null || token.getType() == close) return result;
      if (token.getType() == DepsFileTokenizer.TOKEN_STRING)
        result.add(token.getString());
    }
  }
  
  private void readDepsFile(AbstractCompiler compiler, String filename, Reader reader) throws IOException {
    DepsFileTokenizer tokenizer = new DepsFileTokenizer(compiler, filename, reader);
    while (true) {
      Token token = tokenizer.nextToken();
      if (token == null) return;
      if (token.getType() != DepsFileTokenizer.TOKEN_GOOG_DEPENDENCY) {
        tokenizer.reportError();
        return;
      }
      if (!tokenizer.expect(DepsFileTokenizer.TOKEN_STRING)) return;
      File file = FileUtils.join(referenceDirForDepsFile, new File(tokenizer.lastToken().getString()));
      CompilationUnit compilationUnit = new CompilationUnit(file.getPath(), new CompilationUnitProvider.FromFile(file));
      if (!tokenizer.expect(DepsFileTokenizer.TOKEN_COMA_OPEN_BRACKET)) return;
      Collection<String> providedNames = parseListUntil(tokenizer, DepsFileTokenizer.TOKEN_BRACKET_COMA_BRACKET);
      Collection<String> requiredNames = parseListUntil(tokenizer, DepsFileTokenizer.TOKEN_CLOSE);
      compilationUnit.setDependencies(providedNames, requiredNames);  
      addCompilationUnit(compiler, compilationUnit);
    }
  }

  private void readDepsFile(AbstractCompiler compiler, File file) {
    try {
      Reader reader = new BufferedReader(new FileReader(file));
      readDepsFile(compiler, file.getPath(), reader);
      reader.close();
    } catch (IOException exn) {
      Utils.reportError(compiler, JSError.make(OW_IO_ERROR, exn.getMessage()));            
    }
  }

  // **************************************************************************
  // Writing of deps.js files

  private static void writeJsString(Writer writer, String string) throws IOException {
    writer.write("\"" + StringEscapeUtils.escapeJavaScript(string) + "\"");
  }
  
  private static void writeJsStringList(Writer writer, Iterable<String> strings) throws IOException {
    boolean first = true;
    for (String string: strings) {
      if (first) {
        first = false;
      } else {
        writer.write(", ");
      }
      writeJsString(writer, string);
    }
  }
    
  public void writeDepsFile(Writer writer) throws IOException {
    for (CompilationUnit compilationUnit: compilationUnits) {
      writer.write("goog.addDependency(");
      writeJsString(writer, FileUtils.makeRelative(referenceDirForDepsFile, new File(compilationUnit.getName())).getPath());
      writer.write(", [");
      writeJsStringList(writer, compilationUnit.getProvidedNames());
      writer.write("], [");
      writeJsStringList(writer, compilationUnit.getRequiredNames());
      writer.write("]);\n");
    }
  }
  
  private void writeDepsFile(AbstractCompiler compiler, File file) {
    try {
      Writer writer = new BufferedWriter(new FileWriter(file));
      writeDepsFile(writer);
      writer.close();
    } catch (IOException exn) {
      Utils.reportError(compiler, JSError.make(OW_IO_ERROR, exn.getMessage()));            
    }
  }

}
