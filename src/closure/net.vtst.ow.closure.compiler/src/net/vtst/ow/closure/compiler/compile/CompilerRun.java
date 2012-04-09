package net.vtst.ow.closure.compiler.compile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.vtst.ow.closure.compiler.deps.JSUnit;
import net.vtst.ow.closure.compiler.magic.MagicScopeCreator;
import net.vtst.ow.closure.compiler.util.CompilerUtils;

import com.google.common.collect.Lists;
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
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.rhino.Node;

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

  private String moduleName;
  private List<JSUnit> sortedUnits;
  private Map<JSUnit, Long> lastModifiedMapForFullCompile;
  private Map<JSUnit, Long> lastModifiedMapForFastCompile;

  /**
   * Create a new compiler run.  Note that the call to the constructor triggers the first
   * compilation.
   * @param options  The compilation options
   * @param errorManager  The error manager used to report errors.
   * @param unit  The unit to compile.
   */
  public CompilerRun(String moduleName, CompilerOptions options, ErrorManager errorManager, List<JSUnit> sortedUnits) {
    this.moduleName = moduleName;
    this.options = options;
    this.sortedUnits = sortedUnits;
    // Initializes the compiler and do the first compile
    setupCompiler(errorManager);
    compile();
  }
  
  public void setErrorManager(ErrorManager errorManager) {
    this.compiler.setErrorManager(errorManager);
  }

  // **************************************************************************
  // Full compilation

  private static JSModule buildJSModule(String moduleName, List<JSUnit> sortedUnits) {
    JSModule module = new JSModule(moduleName);
    for (JSUnit unit: sortedUnits) {
      module.add(new CompilerInput(unit.getAst()));
    }
    return module;
  }
  
  public Map<JSUnit, Long> buildLastModifiedMap(List<JSUnit> sortedUnits) {
    Map<JSUnit, Long> map = new HashMap<JSUnit, Long>();
    for (JSUnit unit: sortedUnits) {
      map.put(unit, unit.lastModified());
    }
    return map;
  }

  /**
   * Initialize the JavaScript compiler
   * @param errorManager
   */
  private void setupCompiler(ErrorManager errorManager) {
    compiler = CompilerUtils.makeCompiler(errorManager);
    compiler.initOptions(options);
    passes = new DefaultPassConfig(options);
    CompilerUtils.addCustomCompilerPass(
        options, new NamespaceProvidersPass(compiler, namespaceToScriptNode),
        CustomPassExecutionTime.BEFORE_CHECKS);
    compiler.setPassConfig(passes);
  }

  /**
   * Run the initial compilation.
   */
  private void compile() {
    lastModifiedMapForFullCompile = buildLastModifiedMap(sortedUnits);
    lastModifiedMapForFastCompile = Maps.newHashMap(lastModifiedMapForFullCompile);
    JSModule module = buildJSModule(moduleName, sortedUnits);
    compiler.compileModules(
        Collections.<SourceFile> emptyList(), Lists.newArrayList(DefaultExternsProvider.get(), module), options);
    scopeCreator = new MagicScopeCreator(compiler);
  }
  
  public boolean hasChanged(List<JSUnit> newSortedUnits) {
    if (newSortedUnits.size() != lastModifiedMapForFullCompile.size()) return true;
    for (JSUnit unit: newSortedUnits) {
      Long lastModified = lastModifiedMapForFullCompile.get(unit);
      if (lastModified == null || lastModified.longValue() < unit.lastModified()) return true;
    }
    return false;
  }
  
  // **************************************************************************
  // Fast compilation
  
  /**
   * Run a fast compilation
   */
  public synchronized void fastCompile() {
    // TODO: What happens if one of the units has been deleted?
    for (JSUnit unit: sortedUnits) {
      long current = unit.lastModified();
      Long previous = lastModifiedMapForFastCompile.get(unit);
      assert previous != null;
      if (current > previous.longValue()) {
        lastModifiedMapForFastCompile.put(unit, current);
        JsAst ast = unit.getAst();
        processCustomPassesOnNewScript(ast);
        compiler.replaceScript(ast);
      }
    }
  }
  
  public void processCustomPassesOnNewScript(JsAst ast) {
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

  /**
   * @return  The root node of the compilation result.
   */
  public Node getRoot() {
    return compiler.getRoot();
  }

  /**
   * Get the deepest node for a given offset in a compilation unit.
   * @param compilationUnit  The compilation unit.
   * @param offset  The offset.
   * @return  The node, or null if not found.
   */
  public Node getNode(JSUnit unit, int offset) {
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
}
