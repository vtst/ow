package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.eclipse.easy.ui.properties.editors.ICompositeEditor;

public class ClosureLinterPropertyPage extends ClosureAsbtractPropertyPage {

  @Override
  protected ICompositeEditor createEditor() {
    return new ClosureLinterEditor(this);
  }

}
