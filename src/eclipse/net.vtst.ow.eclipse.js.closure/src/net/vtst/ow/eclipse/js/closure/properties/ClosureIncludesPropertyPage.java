package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.eclipse.easy.ui.properties.editors.ICompositeEditor;

public class ClosureIncludesPropertyPage extends ClosureAsbtractPropertyPage {

  @Override
  protected ICompositeEditor createEditor() {
    return new ClosureIncludesEditor(this);
  }

}
