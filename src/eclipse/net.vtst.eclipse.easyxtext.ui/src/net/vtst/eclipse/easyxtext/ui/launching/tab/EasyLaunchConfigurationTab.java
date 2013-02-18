// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.ui.launching.tab;

import java.util.ArrayList;

import net.vtst.eclipse.easyxtext.util.IEasyMessages;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import com.google.inject.Inject;

/**
 * Base class to implement launch configuration tabs with a
 * {@link net.vtst.eclipse.easyxtext.ui.launching.EasyLaunchConfigurationHelper}.
 * Sub-classes have to implement the methods {@code createControls} and
 * {@code refreshControls}.
 * 
 * @author Vincent Simonet
 * 
 */
public abstract class EasyLaunchConfigurationTab 
  extends AbstractLaunchConfigurationTab
  implements IEasyLaunchConfigurationTab {

  @Inject
  private IEasyMessages messages;
  
  /**
   * Get the bundle to be used for getting messages displayed in the UI.  The default
   * implementation returns the class bound by dependency injection.  Clients may
   * override this.
   * @return the message bundle.
   */
  protected IEasyMessages getMessageBundle() {
    return messages;
  }

  
  // **************************************************************************
  // Functions to be implemented in sub-classes
  
  /**
   * Create the controls for the launch configuration tab.
   * @param parent  The container control in which the controls shall be created.
   */
  public abstract void createControls(Composite parent);
  
  /**
   * This function is called whenever the launch configuration is modified.  Sub-classes
   * may use it to refresh the state of the controls as needed to ensure their consistency.
   */
  protected abstract void refreshControls();
  
  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
   */
  public abstract String getName();

  
  // **************************************************************************
  // Implementation of ILaunchConfigurationTab / AbstractLaunchConfigurationTab
  
  private ArrayList<IEasyLaunchAttributeControl> controls = new ArrayList<IEasyLaunchAttributeControl>();
  private boolean controlCreated = false;

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public void createControl(Composite parent) {
    controlCreated = true;
    Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL);
    setControl(comp);
    createControls(comp);
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
   */
  public void initializeFrom(ILaunchConfiguration config) {
    for (IEasyLaunchAttributeControl control: controls) {
      control.initializeFrom(config);
    }
    refreshControls();
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
   */
  public void performApply(ILaunchConfigurationWorkingCopy config) {
    for (IEasyLaunchAttributeControl control: controls) {
      control.performApply(config);
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
   */
  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    for (IEasyLaunchAttributeControl control: controls) {
      control.setDefaults(config);
    }
    // The method synchronizeControls is not called if the controls have not been created.
    // This is because it seems that setDefaults may be called before createControls
    // when a new launch configuration is created.  Weird.
    if (controlCreated) refreshControls();
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
   */
  public boolean isValid(ILaunchConfiguration config) {
    return true;
  }
  
  // **************************************************************************
  // Implementation of IEasyLaunchConfigurationTab

  /* (non-Javadoc)
   * @see net.vtst.ow.easyxtext.ui.launching.tab.IEasyLaunchConfigurationTab#registerControl(net.vtst.ow.easyxtext.ui.launching.tab.IEasyLaunchAttributeControl)
   */
  public void registerControl(IEasyLaunchAttributeControl control) {
    controls.add(control);
  }

  /* (non-Javadoc)
   * @see net.vtst.ow.easyxtext.ui.launching.tab.IEasyLaunchConfigurationTab#getString(java.lang.String)
   */
  public String getString(String key) {
    return getMessageBundle().getString(key);
  }

  /* (non-Javadoc)
   * @see net.vtst.ow.easyxtext.ui.launching.tab.IEasyLaunchConfigurationTab#getValueChangeListener()
   */
  @Override
  public UpdateListener getUpdateListener() {
    return valueChangeListener;
  }
  
  UpdateListener valueChangeListener = new IEasyLaunchConfigurationTab.UpdateListener() {

    /* (non-Javadoc)
     * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
     */
    public void widgetDefaultSelected(SelectionEvent event) {}

    /* (non-Javadoc)
     * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
     */
    public void widgetSelected(SelectionEvent event) {
      updateLaunchConfigurationDialog();
    }

    /* (non-Javadoc)
     * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
     */
    public void modifyText(ModifyEvent event) {
      updateLaunchConfigurationDialog();
    }
  };
  
}
