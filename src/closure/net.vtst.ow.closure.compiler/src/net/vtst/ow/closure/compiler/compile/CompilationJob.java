package net.vtst.ow.closure.compiler.compile;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.vtst.ow.closure.compiler.deps.CompilationSet;
import net.vtst.ow.closure.compiler.deps.CompilationUnit;
import net.vtst.ow.closure.compiler.magic.MagicScopeCreator;
import net.vtst.ow.closure.compiler.util.CompilerUtils;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.DefaultPassConfig;
import com.google.javascript.jscomp.ErrorManager;
import com.google.javascript.jscomp.JSModule;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.PassConfig;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.Scope;
import com.google.javascript.jscomp.Scope.Var;
import com.google.javascript.rhino.Node;

/**
 * Helper class for compiling files with the Closure compiler and accessing the result.
 * <br/>
 * The typical lifecycle is as follows:
 * <ol>
 *   <li>Construct,</li>
 *   <li>Call compile,</li>
 *   <li>Call the methods to access the result.</li>
 * </ol>
 * @author Vincent Simonet
 */
public class CompilationJob {

  private Compiler compiler;
  private CompilerOptions options;
  private PassConfig passes;
  private MagicScopeCreator scopeCreator;
  
  // **************************************************************************
  // Running the compilation

  /**
   * Create a new job.
   * @param errorManager  The error manager to which the potential errors will be reported.
   */
  public CompilationJob(ErrorManager errorManager) {
    compiler = CompilerUtils.makeCompiler(errorManager);
    options = CompilerUtils.makeOptions();
    options.checkTypes = true;
    compiler.initOptions(options);
    options.closurePass = true;
    passes = new DefaultPassConfig(options);
    compiler.setPassConfig(passes);
  }
  
  public Result compile(CompilationSet<?> compilationSet, Iterable<CompilationUnit> compilationUnits) {
    compilationSet.updateDependencies(compiler);
    JSModule module = compilationSet.makeJSModule(compiler, "test-module", compilationUnits);
    return compiler.compile(new JSSourceFile[]{}, new JSModule[]{module}, options);
  }

  // **************************************************************************
  // Accessing the result
  
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
  public Node getNode(CompilationUnit compilationUnit, int offset) {
    // It would have been cleaner to use the input id to identify the input file, instead of the name.
    // But much more complicated.
    return FindLocationNodeTraversal.findNode(compiler, compiler.getRoot(), compilationUnit.getName(), offset);
  }
  
  /**
   * Private function for lazily initializing the scope creator. 
   */
  private void initScopeCreator() {
    if (scopeCreator == null) scopeCreator = new MagicScopeCreator(passes);;
  }
  
  /**
   * Get the scope for a node (or its first ascendant having a scope).
   * @param node  The node to look for.
   * @return  The scope of the node, or null if not found.
   */
  public Scope getScope(Node node) {
    initScopeCreator();
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
  
}
