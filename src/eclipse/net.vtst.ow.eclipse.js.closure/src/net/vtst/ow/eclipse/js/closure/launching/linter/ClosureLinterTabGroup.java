package net.vtst.ow.eclipse.js.closure.launching.linter;

import net.vtst.eclipse.easy.ui.properties.editors.ICompositeEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditor;
import net.vtst.eclipse.easy.ui.properties.editors.IEditorContainer;
import net.vtst.eclipse.easy.ui.properties.pages.EasyLaunchConfigurationTab;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.launching.compiler.ClosureCompilerInputsAndOutputsEditor;
import net.vtst.ow.eclipse.js.closure.launching.compiler.ClosureCompilerProjectPropertiesEditor;
import net.vtst.ow.eclipse.js.closure.properties.ClosureCompilerChecksEditor;
import net.vtst.ow.eclipse.js.closure.properties.ClosureIncludesEditor;
import net.vtst.ow.eclipse.js.closure.properties.ClosureLinterChecksEditor;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

public class ClosureLinterTabGroup extends AbstractLaunchConfigurationTabGroup {
  
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
    final ClosureLinterLaunchConfigurationRecord record = new ClosureLinterLaunchConfigurationRecord(); 
    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
        new Tab("ClosureLinterOptions"){
          protected ICompositeEditor createEditor() {
            return new ClosureLinterOptionsEditor(this);
        }},
        new Tab("ClosureLinterChecks", "ClosureProjectPropertyPage_"){
          protected ICompositeEditor createEditor() {
            return new ClosureCompilerProjectPropertiesEditor(this, record.useProjectPropertiesForLinterChecks) {
              public IEditor createDelegate(IEditorContainer container) {
                return new ClosureLinterChecksEditor(container);
              }
            };
        }},
        new EnvironmentTab(),
        new CommonTab()
    };
    setTabs(tabs);
  }

}
