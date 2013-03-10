package net.vtst.ow.eclipse.soy.nature;

import net.vtst.eclipse.easyxtext.nature.IEasyProjectNature;
import net.vtst.eclipse.easyxtext.nature.NullProjectNature;
import net.vtst.ow.eclipse.soy.SoyMessages;

import com.google.inject.Inject;

public class SoyProjectNature extends NullProjectNature implements IEasyProjectNature {

  private static final String NATURE_ID = "net.vtst.ow.eclipse.soy.nature";
  
  @Inject
  SoyMessages messages;

  public String getId() {
    return NATURE_ID;
  }

  public String getName() {
    return messages.getString("soy_project_nature");
  }
}
