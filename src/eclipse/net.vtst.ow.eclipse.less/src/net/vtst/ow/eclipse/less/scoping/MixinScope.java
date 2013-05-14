package net.vtst.ow.eclipse.less.scoping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.MultimapBasedScope;

/**
 * Scope for a mixin call.  The scope is defined for a given list of selectors.  For every selector,
 * a list of scope elements is provided.
 * 
 * @author Vincent Simonet
 */
public class MixinScope {
  
  /**
   * A list of scope elements which can be lazily converted into an {@code IScope}.
   */
  private class LazyScope {
    private ArrayList<MixinScopeElement> elements = new ArrayList<MixinScopeElement>();
    private IScope scope = null;
    private int position;
    
    private LazyScope(int position) { this.position = position; }
    
    private void add(MixinScopeElement element) {
      elements.add(element);
      scope = null;
    }
    
    private Iterable<IEObjectDescription> getEObjectDescriptions() {
      ArrayList<IEObjectDescription> result = new ArrayList<IEObjectDescription>(elements.size());
      for (MixinScopeElement element : elements) {
        result.add(element.asEObjectDescription(position));
      }
      return result;
    }
    
    private List<MixinScopeElement> getElements() { return this.elements; }
    
    private IScope get() {
      if (scope == null) {
        IScope parentScope = parent == null ? IScope.NULLSCOPE : parent.getScope(this.position);
        scope = MultimapBasedScope.createScope(parentScope, getEObjectDescriptions(), false);
      }
      return scope;
    }
  }
  
  private MixinScope parent;
  private MixinPath path;
  private ArrayList<LazyScope> scopes;
  private ArrayList<MixinScopeElement> fullMatches = new ArrayList<MixinScopeElement>();
  
  private MixinScope(MixinPath selectors, MixinScope parent) {
    this.parent = parent;
    this.path = selectors;
    this.scopes = new ArrayList<LazyScope>(selectors.size());
    for (int i = 0; i < selectors.size(); ++i) {
      this.scopes.add(new LazyScope(i));
    }
  }
  
  /**
   * Create a root scope.
   */
  public MixinScope(MixinPath selectors) {
    this(selectors, null);
  }
  
  /**
   * Create a sub-scope.
   */
  public MixinScope(MixinScope parent) {
    this(parent.path, parent);
  }

  public MixinPath getPath() { return path; }

  public void addAtPosition(int position, MixinScopeElement element) {
    this.scopes.get(position).add(element);
  }

  public IScope getScope(int position) {
    return scopes.get(position).get();
  }
  
  private void fillCompletionProposals(Map<String, MixinScopeElement> elements, int position) {
    if (parent != null) parent.fillCompletionProposals(elements, position);
    for (MixinScopeElement element : scopes.get(position).getElements()) {
      elements.put(element.getName(), element);
    }    
  }

  public Iterable<MixinScopeElement> getCompletionProposals(int position) {
    Map<String, MixinScopeElement> elements = new TreeMap<String, MixinScopeElement>();
    fillCompletionProposals(elements, position);
    // TODO: Need sorting.
    return elements.values();
  }

  public void addFullMatch(MixinScopeElement element) {
    this.fullMatches.add(element);
  }
  
  // TODO: Can we avoid creating a data structure?
  private void fillFullMatches(List<MixinScopeElement> accu) {
    if (parent != null) parent.fillFullMatches(accu);
    accu.addAll(this.fullMatches);
  }

  public Iterable<MixinScopeElement> getFullMatches() {
    if (this.parent == null) return this.fullMatches;
    List<MixinScopeElement> result = new ArrayList<MixinScopeElement>();
    fillFullMatches(result);
    return result;
  }
  
}
