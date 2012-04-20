package net.vtst.ow.eclipse.js.closure.properties.project;

import net.vtst.eclipse.easy.ui.properties.editors.ICompositeEditor;

public class ClosureCompilerChecksPropertyPage extends ClosureAsbtractPropertyPage {

  @Override
  protected ICompositeEditor createEditor() {
    return new ClosureCompilerChecksEditor(this);
  }

}
