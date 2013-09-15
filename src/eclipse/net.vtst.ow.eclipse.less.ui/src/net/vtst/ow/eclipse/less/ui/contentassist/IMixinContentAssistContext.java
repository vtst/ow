package net.vtst.ow.eclipse.less.ui.contentassist;

import net.vtst.ow.eclipse.less.scoping.MixinPath;

public interface IMixinContentAssistContext {
  
  public boolean isValid();

  public MixinPath getPath();

  public int getIndex();

}
