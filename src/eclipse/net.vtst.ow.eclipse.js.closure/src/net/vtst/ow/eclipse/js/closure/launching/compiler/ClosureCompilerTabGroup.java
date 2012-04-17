package net.vtst.ow.eclipse.js.closure.launching.compiler;

import net.vtst.eclipse.easy.ui.properties.editors.ICompositeEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.pages.EasyLaunchConfigurationTab;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.properties.ClosureCompilerChecksEditor;
import net.vtst.ow.eclipse.js.closure.properties.ClosureIncludesEditor;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

public class ClosureCompilerTabGroup extends AbstractLaunchConfigurationTabGroup {
  
  private final OwJsClosureMessages messages = OwJsClosurePlugin.getDefault().getMessages();

  public abstract class Tab extends EasyLaunchConfigurationTab {
    private String nameKey;
    private String keyPrefix;
    public Tab(String nameKey) {
      this(nameKey, nameKey + "_");
    }
    
    public Tab(String nameKey, String keyPrefix) {
      this.nameKey = nameKey;
      this.keyPrefix = keyPrefix;
    }
    
    @Override
    public String getMessage(String key) {
      return messages.getStringOrNull(keyPrefix + key);
    }

    @Override
    public String getName() {
      return messages.getString(nameKey);
    }    
  }

  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    final ClosureCompilerLaunchConfigurationRecord record = new ClosureCompilerLaunchConfigurationRecord(); 
    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
        new Tab("ClosureCompilerInputsAndOutputs"){
          protected ICompositeEditor createEditor() {
            return new ClosureCompilerInputsAndOutputsEditor(this);
        }},
        new Tab("ClosureCompilerCompilationOptions"){
          protected ICompositeEditor createEditor() {
            return new ClosureCompilerCompilationOptionsEditor(this);
        }},
        new Tab("ClosureCompilerIncludes", "ClosureProjectPropertyPage_"){
          protected ICompositeEditor createEditor() {
            return new ClosureCompilerProjectPropertiesEditor(this, record.useProjectPropertiesForIncludes) {
              public IEditor createDelegate(IEditorContainer container) {
                return new ClosureIncludesEditor(container);
              }
            };
        }},
        new Tab("ClosureCompilerChecks", "ClosureProjectPropertyPage_"){
          protected ICompositeEditor createEditor() {
            return new ClosureCompilerProjectPropertiesEditor(this, record.useProjectPropertiesForChecks) {
              public IEditor createDelegate(IEditorContainer container) {
                return new ClosureCompilerChecksEditor(container);
              }
            };
        }},
        new CommonTab()
    };
    setTabs(tabs);
  }

}
