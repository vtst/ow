package net.vtst.ow.eclipse.less.less;

/**
 * Helper class for manipulating Mixins.
 */
public class MixinUtils {
  
  public static abstract class Helper {
    public abstract MixinSelectors getSelectors();
    public abstract MixinParameters getParameters();
    public abstract MixinDefinitionGuards getGuard();
    public abstract Block getBody();   
    
    public boolean isDefinition() {
      return this.getBody() == null;
    }
    
    public boolean isCall() {
      return this.getBody() != null;
    }
  }
  
  public static class HelperForTerminatedMixin extends Helper {
    private TerminatedMixin mixin;

    public HelperForTerminatedMixin(TerminatedMixin mixin) {
      this.mixin = mixin;
    }
    
    public MixinSelectors getSelectors() { return mixin.getSelectors(); }
    public MixinParameters getParameters() { return mixin.getParameters(); }
    public MixinDefinitionGuards getGuard() { return mixin.getGuards(); }
    public Block getBody() { return mixin.getBody(); }
  }
  
  public static class HelperForUnterminatedMixin extends Helper {
    private UnterminatedMixin mixin;

    public HelperForUnterminatedMixin(UnterminatedMixin mixin) {
      this.mixin = mixin;
    }
    
    public MixinSelectors getSelectors() { return mixin.getSelectors(); }
    public MixinParameters getParameters() { return null; }
    public MixinDefinitionGuards getGuard() { return null; }
    public Block getBody() { return null; }
  }


  public static Helper newHelper(TerminatedMixin mixin) { return new HelperForTerminatedMixin(mixin); }
  public static Helper newHelper(UnterminatedMixin mixin) { return new HelperForUnterminatedMixin(mixin); }
  public static Helper newHelper(Mixin mixin) {
    if (mixin instanceof UnterminatedMixin) return newHelper((UnterminatedMixin) mixin);
    if (mixin instanceof TerminatedMixin) return newHelper((TerminatedMixin) mixin);
    throw new RuntimeException("Unknown sub-class of Mixin");
  }
}
