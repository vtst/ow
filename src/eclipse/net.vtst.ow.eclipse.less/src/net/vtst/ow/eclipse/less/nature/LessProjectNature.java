package net.vtst.ow.eclipse.less.nature;

import com.google.inject.Inject;

import net.vtst.eclipse.easyxtext.nature.IEasyProjectNature;
import net.vtst.eclipse.easyxtext.nature.NullProjectNature;
import net.vtst.ow.eclipse.less.LessMessages;

public class LessProjectNature extends NullProjectNature implements IEasyProjectNature {
  
  private static final String NATURE_ID = "net.vtst.ow.eclipse.less.nature";
  
  @Inject
  LessMessages messages;

  @Override
  public String getId() {
    return NATURE_ID;
  }

  @Override
  public String getName() {
    return messages.getString("less_project_nature");
  }

}
