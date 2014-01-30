package net.vtst.eclipse.easyxtext.ui.validation.config;

import net.vtst.eclipse.easyxtext.ui.validation.config.AbstractValidatorPropertyPage.IStore;
import net.vtst.eclipse.easyxtext.validation.config.DeclarativeValidatorInspector.Group;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.google.inject.Inject;

public class ValidatorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  @Inject
  private AbstractValidatorPropertyPage base;
  
  @Override
  protected Control createContents(Composite parent) {
    base.init(null, this.getShell(), new IStore() {
      public boolean getEnabled(Group group) throws CoreException {
        return base.getInspector().getEnabled(getPreferenceStore(), group);
      }

      public void setEnabled(Group group, boolean enabled) throws CoreException {
        base.getInspector().setEnabled(getPreferenceStore(), group, enabled);        
      }

      @Override
      public boolean getCustomized() throws CoreException {
        return base.getInspector().getCustomized(getPreferenceStore());
      }

      @Override
      public void setCustomized(boolean customized) throws CoreException {
        base.getInspector().setCustomized(getPreferenceStore(), customized);
      }
    });
    return this.base.createContents(parent);
  }
  
  @Override
  protected void performDefaults() {
    base.performDefaults();
    super.performDefaults();
  }
  
  @Override
  public boolean performOk() {
    System.out.println(this.getPreferenceStore());
    return base.performOk() && super.performOk();
  }

  @SuppressWarnings("deprecation")
  public void init(IWorkbench workbench) {
    this.setPreferenceStore(workbench.getPreferenceStore());
  }
  
}
