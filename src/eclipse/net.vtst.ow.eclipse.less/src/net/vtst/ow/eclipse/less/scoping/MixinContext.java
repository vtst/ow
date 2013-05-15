package net.vtst.ow.eclipse.less.scoping;

import net.vtst.ow.eclipse.less.less.HashOrClassRef;
import net.vtst.ow.eclipse.less.less.Mixin;
import net.vtst.ow.eclipse.less.less.MixinSelectors;
import net.vtst.ow.eclipse.less.less.MixinUtils;

import org.eclipse.emf.ecore.EObject;

public class MixinContext {
  
  private boolean isValid;
  private HashOrClassRef hashOrClass;
  private int selectorIndex = 0;
  private Mixin mixin;
  private MixinUtils.Helper mixinHelper;
  
  public MixinContext(EObject hashOrClass) {
    this.isValid = initEObjects(hashOrClass) && initSelectorIndex();
  }

  private boolean initEObjects(EObject hashOrClass) {
    if (!(hashOrClass instanceof HashOrClassRef)) return false;
    this.hashOrClass = (HashOrClassRef) hashOrClass;
    EObject container1 = hashOrClass.eContainer();
    if (!(container1 instanceof MixinSelectors)) return false;
    EObject container2 = container1.eContainer();
    if (!(container2 instanceof Mixin)) return false;
    this.mixin = (Mixin) container2;
    this.mixinHelper = MixinUtils.newHelper(this.mixin);
    return !this.mixinHelper.isDefinition();
  }

  private boolean initSelectorIndex() {
    for (HashOrClassRef item: mixinHelper.getSelectors().getSelector()) {
      if (item == this.hashOrClass) return true;
      ++this.selectorIndex;
    }
    return false;
  }

  public int getSelectorIndex() { return this.selectorIndex; }  
  public Mixin getMixin() { return this.mixin; }
  public MixinSelectors getMixinSelectors() { return this.mixinHelper.getSelectors(); }
  public MixinUtils.Helper getMixinHelper() { return this.mixinHelper; }
  public boolean isValid() { return this.isValid; }

}
