package net.vtst.ow.eclipse.less.scoping;

import java.util.ArrayList;

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
    
    private IScope get() {
      if (scope == null) {
        IScope parentScope = parent == null ? IScope.NULLSCOPE : parent.getScope(this.position);
        scope = MultimapBasedScope.createScope(parentScope, getEObjectDescriptions(), false);
      }
      return scope;
    }
  }
  
  private MixinScope parent;
  private MixinSelectors selectors;
  private ArrayList<LazyScope> scopes;
  
  private MixinScope(MixinSelectors selectors, MixinScope parent) {
    this.parent = parent;
    this.selectors = selectors;
    this.scopes = new ArrayList<LazyScope>(selectors.size());
    for (int i = 0; i < selectors.size(); ++i) {
      this.scopes.add(new LazyScope(i));
    }
  }
  
  /**
   * Create a root scope.
   */
  public MixinScope(MixinSelectors selectors) {
    this(selectors, null);
  }
  
  /**
   * Create a sub-scope.
   */
  public MixinScope(MixinScope parent) {
    this(parent.selectors, parent);
  }

  public MixinSelectors getSelectors() { return selectors; }

  public void addAtPosition(int position, MixinScopeElement element) {
    this.scopes.get(position).add(element);
  }

  public IScope getScope(int position) {
    return scopes.get(position).get();
  }
  
}
