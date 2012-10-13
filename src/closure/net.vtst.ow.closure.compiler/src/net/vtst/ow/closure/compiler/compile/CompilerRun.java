package net.vtst.ow.closure.compiler.compile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import net.vtst.ow.closure.compiler.deps.JSExtern;
import net.vtst.ow.closure.compiler.deps.JSUnit;
import net.vtst.ow.closure.compiler.magic.MagicCompiler;
import net.vtst.ow.closure.compiler.magic.MagicScopeCreator;
import net.vtst.ow.closure.compiler.util.CompilerUtils;

import com.google.common.collect.Maps;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerInput;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerPass;
import com.google.javascript.jscomp.CustomPassExecutionTime;
import com.google.javascript.jscomp.DefaultPassConfig;
import com.google.javascript.jscomp.ErrorManager;
import com.google.javascript.jscomp.HotSwapCompilerPass;
import com.google.javascript.jscomp.JSModule;
import com.google.javascript.jscomp.JsAst;
import com.google.javascript.jscomp.PassConfig;
import com.google.javascript.jscomp.Scope;
import com.google.javascript.jscomp.Scope.Var;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.jstype.JSType;
import com.google.javascript.rhino.jstype.ObjectType;

/**
 * Wrapper class to handle the compilation of a JS unit, and to provide the results.
 * <br>
 * <b>Thread safety:</b>  Full compilation can be performed only once in the class life
 * cycle.  Fast compilation is synchronized.
 * @author Vincent Simonet
 */
public class CompilerRun {
  
  private Compiler compiler;
  private CompilerOptions options;
  private PassConfig passes;
  private MagicScopeCreator scopeCreator;
  private NamespaceProvidersMap namespaceToScriptNode = new NamespaceProvidersMap();
  private boolean keepCompilationResultsInMemory = true;

  private String moduleName;
  private List<JSUnit> sortedUnits;
  private Collection<JSExtern> externs;
  private Collection<JSUnit> entryPoints;
  private boolean stripIncludedFiles;
  private Map<JSUnit, Long> lastModifiedMapForFullCompile;
  private Map<JSUnit, Long> lastModifiedMapForFastCompile;

  /**
   * Create a new compiler run.  Note that the call to the constructor triggers the first
   * compilation.
   * @param options  The compilation options
   * @param errorManager  The error manager used to report errors.
   * @param sortedUnits  The unit to compile.
   * @param entryPoints  A subset of {@code sortedUnits}, these units will not be stripped.
   */
  public CompilerRun(
      String moduleName, CompilerOptions options, ErrorManager errorManager, 
      Collection<JSExtern> externs, List<JSUnit> sortedUnits, Collection<JSUnit> entryPoints,
      boolean keepCompilationResultsInMemory, boolean stripIncludedFiles) {
    this.moduleName = moduleName;
    this.options = options;
    this.externs = externs;
    this.sortedUnits = sortedUnits;
    this.entryPoints = entryPoints;
    this.keepCompilationResultsInMemory = keepCompilationResultsInMemory;
    this.stripIncludedFiles = stripIncludedFiles; 
    // Initializes the compiler and do the first compile
    compile(errorManager);
  }
  
  public void setErrorManager(ErrorManager errorManager) {
    if (compiler == null) return;
    compiler.setErrorManager(errorManager);
  }
  
  private boolean shouldStrip(JSUnit unit) {
    return stripIncludedFiles && !entryPoints.contains(unit);
  }

  // **************************************************************************
  // Full compilation

  private JSModule buildJSModule() {
    JSModule module = new JSModule(moduleName);
    for (JSUnit unit: sortedUnits) {
      module.add(new CompilerInput(unit.getAst(shouldStrip(unit))));
    }
    return module;
  }
  
  private Map<JSUnit, Long> buildLastModifiedMap(List<JSUnit> sortedUnits) {
    Map<JSUnit, Long> map = new HashMap<JSUnit, Long>();
    for (JSUnit unit: sortedUnits) {
      map.put(unit, unit.lastModified());
    }
    return map;
  }

  /**
   * Run the initial compilation.
   */
  private void compile(ErrorManager errorManager) {
    // Set-up the compiler
    compiler = CompilerUtils.makeCompiler(errorManager);
    compiler.initOptions(options);
    passes = new DefaultPassConfig(options);
    CompilerUtils.addCustomCompilerPass(
        options, new NamespaceProvidersPass(compiler, namespaceToScriptNode),
        CustomPassExecutionTime.BEFORE_CHECKS);
    compiler.setPassConfig(passes);
    
    // Store the modification info
    lastModifiedMapForFullCompile = buildLastModifiedMap(sortedUnits);
    lastModifiedMapForFastCompile = Maps.newHashMap(lastModifiedMapForFullCompile);

    // Run compilation
    JSModule module = buildJSModule();
    // For avoiding the magic, we could do:
    // compiler.compileModules(
    //   Collections.<SourceFile> emptyList(), Lists.newArrayList(DefaultExternsProvider.getAsModule(), module), options);
    // but this would be less efficient.
    MagicCompiler.compile(compiler, getExternsAsCompilerInputs(), module, options);
    
    // Set-up post-compilation state
    if (keepCompilationResultsInMemory) {
      scopeCreator = new MagicScopeCreator(compiler);
    } else {
      compiler = null;
      passes = null;
      options = null;
    }
  }
  
  public boolean hasChanged(List<JSUnit> newSortedUnits) {
    if (newSortedUnits.size() != lastModifiedMapForFullCompile.size()) return true;
    for (JSUnit unit: newSortedUnits) {
      Long lastModified = lastModifiedMapForFullCompile.get(unit);
      if (lastModified == null || lastModified.longValue() < unit.lastModified()) return true;
    }
    return false;
  }
  
  private List<CompilerInput> getExternsAsCompilerInputs() {
    ArrayList<CompilerInput> result = new ArrayList<CompilerInput>(externs.size());
    for (JSExtern extern: externs) result.add(new CompilerInput(extern.getClone(false), true));
    return result;
  }
  
  // **************************************************************************
  // Fast compilation
  
  /**
   * Run a fast compilation
   */
  public synchronized void fastCompile() {
    if (!keepCompilationResultsInMemory) return;
    // This should work even if one of the unit has been deleted, because in that case
    // its provider will return an empty source code.
    for (JSUnit unit: sortedUnits) {
      long current = unit.lastModified();
      Long previous = lastModifiedMapForFastCompile.get(unit);
      assert previous != null;
      if (current > previous.longValue()) {
        lastModifiedMapForFastCompile.put(unit, current);
        JsAst ast = unit.getAst(shouldStrip(unit));
        processCustomPassesOnNewScript(ast);
        compiler.replaceScript(ast);
      }
    }
  }
  
  private void processCustomPassesOnNewScript(JsAst ast) {
    if (!keepCompilationResultsInMemory) return;
    if (options.customPasses == null) return;
    Node scriptRoot = ast.getAstRoot(compiler);
    Node originalRoot = compiler.getRoot();
    for (CompilerPass pass: options.customPasses.get(CustomPassExecutionTime.BEFORE_CHECKS)) {
      if (pass instanceof HotSwapCompilerPass) {
        ((HotSwapCompilerPass) pass).hotSwapScript(scriptRoot, originalRoot);
      }
    }
  }

  // **************************************************************************
  // Accessing to the result of the compilation
  
  public boolean getKeepCompilationResultsInMemory() {
    return keepCompilationResultsInMemory;
  }

  /**
   * @return  The root node of the compilation result.
   */
  public Node getRoot() {
    if (!keepCompilationResultsInMemory) return null;
    return compiler.getRoot();
  }

  /**
   * Get the deepest node for a given offset in a compilation unit.
   * @param compilationUnit  The compilation unit.
   * @param offset  The offset.
   * @return  The node, or null if not found.
   */
  public Node getNode(JSUnit unit, int offset) {
    if (!keepCompilationResultsInMemory) return null;
    // It would have been cleaner to use the input id to identify the input file, instead of the name.
    // But much more complicated.
    return FindLocationNodeTraversal.findNode(compiler, compiler.getRoot(), unit.getName(), offset);
  }
  
  /**
   * Get the scope for a node (or its first ascendant having a scope).
   * @param node  The node to look for.
   * @return  The scope of the node, or null if not found.
   */
  public Scope getScope(Node node) {
    if (!keepCompilationResultsInMemory) return null;
    while (node != null) {
      Scope scope = scopeCreator.getScope(node);
      if (scope != null) return scope;
      node = node.getParent();
    }
    return null;    
  }
  
  /**
   * Get all the symbols defined by a scope and its ascendant.  Only the visible symbols (i.e. those which
   * are not overridden by another symbol with the same name in a closest context) are returned.
   * @param scope
   * @return  The defined symbols.
   */
  private static Iterable<Var> getAllSymbolsRecursively(Scope scope) {
    Set<String> names = new HashSet<String>();
    Collection<Var> vars = new ArrayList<Var>();
    while (scope != null) {
      for (Var var: scope.getAllSymbols()) {
        if (names.add(var.getName())) vars.add(var);
      }
      scope = scope.getParent();
    }
    return vars;
  }
  
  /**
   * Get all the symbols defined for a given node.
   * @param node  The node.
   * @return  The defined symbols.
   */
  public Iterable<Var> getAllSymbols(Node node) {
    return getAllSymbolsRecursively(getScope(node));
  }
  
  /**
   * Get the script node which provides a name space.
   * @param namespace
   * @return  The script node, or null.
   */
  public Node getNamespaceProvider(String namespace) {
    return namespaceToScriptNode.get(namespace);
  }
  
  private static String THIS = "this";
  
  /**
   * Return the type of the object designated by a name in the given scope.
   * Takes care of "this".
   * @param scope  The scope to look in.
   * @param name  The name to look at.
   * @return  The type, or null.
   */
  private static JSType getTypeOfName(Scope scope, String name) {
    if (THIS.equals(name)) return scope.getTypeOfThis();
    Var var = scope.getVar(name);
    if (var == null) return null;
    else return var.getType();
  }
  
  /**
   * Get the type of the object designated by a qualified name in a given scope.
   * @param scope  The scope to look in.
   * @param qualifiedName  The qualified name to look at.
   * @return  The type, or null.
   */
  public static JSType getTypeOfQualifiedName(Scope scope, List<String> qualifiedName) {
    ListIterator<String> it = qualifiedName.listIterator();
    if (!it.hasNext()) return null;
    JSType type = getTypeOfName(scope, it.next());
    while (it.hasNext()) {
      if (!(type instanceof ObjectType)) return null;
      type = ((ObjectType) type).getPropertyType(it.next());
    }
    return type;
  }
}
