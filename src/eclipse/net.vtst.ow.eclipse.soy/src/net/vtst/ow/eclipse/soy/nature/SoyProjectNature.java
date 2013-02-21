package net.vtst.ow.eclipse.soy.nature;

import com.google.inject.Inject;

import net.vtst.eclipse.easyxtext.nature.IEasyProjectNature;
import net.vtst.eclipse.easyxtext.nature.NullProjectNature;
import net.vtst.ow.eclipse.soy.SoyMessages;

public class SoyProjectNature extends NullProjectNature implements IEasyProjectNature {

  private static final String NATURE_ID = "net.vtst.ow.eclipse.soy.nature";
  
  @Inject
  SoyMessages messages;

  @Override
  public String getId() {
    return NATURE_ID;
  }

  @Override
  public String getName() {
    return messages.getString("soy_project_nature");
  }
}
