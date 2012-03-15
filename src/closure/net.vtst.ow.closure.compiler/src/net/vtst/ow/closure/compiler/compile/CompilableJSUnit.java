package net.vtst.ow.closure.compiler.compile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.vtst.ow.closure.compiler.deps.JSSet;
import net.vtst.ow.closure.compiler.deps.JSUnit;
import net.vtst.ow.closure.compiler.deps.JSUnitProvider.Interface;
import net.vtst.ow.closure.compiler.magic.MagicScopeCreator;
import net.vtst.ow.closure.compiler.util.CompilerUtils;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerInput;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.DefaultPassConfig;
import com.google.javascript.jscomp.ErrorManager;
import com.google.javascript.jscomp.JSModule;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.PassConfig;
import com.google.javascript.jscomp.Scope;
import com.google.javascript.jscomp.Scope.Var;
import com.google.javascript.rhino.Node;

public class CompilableJSUnit extends JSUnit {

  private Map<String, Long> lastModified = new HashMap<String, Long>();
  private Compiler compiler;
  private CompilerOptions options;
  private PassConfig passes;
  private MagicScopeCreator scopeCreator;
  private ErrorManager errorManager;
  private JSSet<?> compilationSet;

  public CompilableJSUnit(
      ErrorManager errorManager, JSSet<?> compilationSet, 
      File path, File pathOfClosureBase, Interface provider) {
    super(path, pathOfClosureBase, provider);
    this.errorManager = errorManager;
    this.compilationSet = compilationSet;
  }

  // **************************************************************************
  // Compilation
  
  private void initCompiler() {
    compiler = CompilerUtils.makeCompiler(errorManager);
    options = CompilerUtils.makeOptions();
    options.checkTypes = true;
    compiler.initOptions(options);
    options.closurePass = true;
    passes = new DefaultPassConfig(options);
    compiler.setPassConfig(passes);    
  }
    
  private void fullCompile() {
    initCompiler();
    compilationSet.updateDependencies(compiler);
    List<JSUnit> units = compilationSet.getRequiredJSUnits(compiler, Collections.singleton(this));
    JSModule module = new JSModule(this.getName());
    for (JSUnit unit: units) {
      lastModified.put(unit.getName(), unit.lastModified());
      module.add(new CompilerInput(unit.getAst()));      
    }
    compiler.compile(new JSSourceFile[]{}, new JSModule[]{module}, options);
  }
  
  private void incrementalCompile() {
    compilationSet.updateDependencies(compiler);
    List<JSUnit> units = compilationSet.getRequiredJSUnits(compiler, Collections.singleton(this));
    for (JSUnit unit: units) {
      long current = unit.lastModified();
      Long previous = lastModified.get(unit.getName());
      if (previous == null) {
        lastModified.put(unit.getName(), current);
        compiler.addNewScript(unit.getAst());
      } else if (current > previous) {
        lastModified.put(unit.getName(), current);
        compiler.replaceScript(unit.getAst());
      }
    }    
  }
  
  public void compile() {
    if (compiler == null) fullCompile();
    else incrementalCompile();
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
  public Node getNode(int offset) {
    // It would have been cleaner to use the input id to identify the input file, instead of the name.
    // But much more complicated.
    return FindLocationNodeTraversal.findNode(compiler, compiler.getRoot(), getName(), offset);
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
