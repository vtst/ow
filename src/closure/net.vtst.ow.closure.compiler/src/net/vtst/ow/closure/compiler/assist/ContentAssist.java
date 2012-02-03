package net.vtst.ow.closure.compiler.assist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.vtst.ow.closure.compiler.magic.MagicScopeCreator;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.PassConfig;
import com.google.javascript.jscomp.Scope;
import com.google.javascript.jscomp.Scope.Var;
import com.google.javascript.rhino.Node;

public class ContentAssist {

  public static Scope getScope(AbstractCompiler compiler, PassConfig passes, Node node) {
    MagicScopeCreator scopeCreator = new MagicScopeCreator(passes);
    while (node != null) {
      Scope scope = scopeCreator.getScope(node);
      if (scope != null) return scope;
      node = node.getParent();
    }
    return null;
  }
  
  public static Iterable<Var> getAllSymbolsRecursively(Scope scope) {
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
  
  public static Iterable<String> getContentProposals(AbstractCompiler compiler, PassConfig passes, Node node, String prefix) {
    Collection<String> result = new ArrayList<String>();
    Scope scope = getScope(compiler, passes, node);
    if (scope == null) return result;
    Iterable<Var> vars = getAllSymbolsRecursively(scope);
    for (Var var: vars) {
      result.add(var.getName());
    }
    return result;
  }
  
}
