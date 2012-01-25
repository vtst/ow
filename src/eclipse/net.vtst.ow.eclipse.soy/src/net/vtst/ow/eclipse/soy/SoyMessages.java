package net.vtst.ow.eclipse.soy;

import java.util.ResourceBundle;

import net.vtst.eclipse.easyxtext.util.EasyResourceBundle;

import com.google.inject.Singleton;

@Singleton
public class SoyMessages extends EasyResourceBundle {
  public ResourceBundle getBundle() {
    return ResourceBundle.getBundle("net.vtst.ow.eclipse.soy.SoyMessages");
  }
}
