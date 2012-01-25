// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.ui.launching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.vtst.eclipse.easyxtext.ui.Activator;
import net.vtst.eclipse.easyxtext.ui.EasyXtextUiMessages;
import net.vtst.eclipse.easyxtext.ui.util.MiscUi;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.statushandlers.StatusManager;

import com.google.inject.Inject;

/**
 * Abstract class for implementing a launch configuration shortcut.
 * <p>
 * Class to implement a {@link org.eclipse.debug.ui.ILaunchShortcut} with some
 * support for:
 * </p>
 * <ul>
 *   <li>Getting the current selection as an iterable of resources,</li>
 *   <li>Finding the right launch configuration for a selection.</li>
 * </ul>
 * @author Vincent Simonet
 *
 * @param <Selection>  Class to store the launched selection in a structured
 *   way.  See <code>getSelection</code>.
 */
public abstract class EasyLaunchShortcut<Selection> implements ILaunchShortcut {

  @Inject
  EasyXtextUiMessages messages;
  
  /**
   * This class computes the selection as an {@code Iterable<IResource>}.  However, given
   * the enablement condition for the shortcut, the selection may be represented by a simpler
   * object (e.g. one may be sure that the iterable will contain exactly one {@code IFile} object,
   * hence it is simpler to manipulate it as an {@code IFile} object. <br/>
   * For this purpose, the current class is parameterized by the {@code Selection} class (which
   * would be {@code IFile} in the previous example).  The method {@code getSelection} shall
   * perform the conversion.
   * @param selection  The selection computed by the base class.
   * @return  the structured selection.
   * @throws CoreException
   */
  protected abstract Selection getSelection(Iterable<IResource> selection) throws CoreException;

  
  // **************************************************************************
  // Getting the launch configuration for a selection

  /**
   * Get the ID of the launch configuration type used by this launch shortcut.
   * @return The ID of the launch configuration type.
   */
  protected abstract String getLaunchConfigurationTypeID();
  
  /**
   * Get the launch configuration type used by this launch shortcut.  The default implementation
   * return the launch configuration type corresponding to the ID given by
   * {@link #getLaunchConfigurationTypeID()}.  Sub-classes may override this behavior.
   * @return  The launch configuration type.
   */
  protected ILaunchConfigurationType getLaunchConfigurationType() {
    return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(getLaunchConfigurationTypeID());
  }
  
  /**
   * Get the launch configurations which may be used for launching the shortcut, 
   * in a selection-independent way.  The default implementation returns all launch configurations
   * of the type returned by <code>getLaunchConfigurationType</code>.  Sub-classes may override this
   * behavior.
   * @return the array of launch configurations.
   * @throws CoreException
   */
  protected ILaunchConfiguration[] getAllLaunchConfigurations() throws CoreException {
    return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(getLaunchConfigurationType());
  }
    
  /**
   * Return the launch configurations which may be used to launch the given selection.
   * @param selection  The selection to launch.
   * @param mode  The launch mode.
   * @return  A list of candidate launch configurations.  May be empty, but not null.
   * @throws CoreException 
   */
  protected abstract List<ILaunchConfiguration> findLaunchConfigurations(Selection selection, String mode) throws CoreException;
  
  /**
   * Create a new launch configuration for the given selection.
   * The default implementation creates a new instance of the suitable type.
   * @param selection  The selection to launch.
   * @param mode  The launch mode.
   * @return  A new launch configuration.
   * @throws CoreException
   */
  protected ILaunchConfiguration createLaunchConfiguration(Selection selection, String mode) throws CoreException {
    return getLaunchConfigurationType().newInstance(null, DebugPlugin.getDefault().getLaunchManager().generateLaunchConfigurationName("Auto-created"));    
  }
  
  /**
   * Adapt a launch configuration for a given selection.  If the configuration passed
   * as argument is already well suited for the selection, it may be returned directly.
   * Otherwise, a new working copy may be created, modified, and returned.
   * @param config  The source launch configuration.
   * @param selection  The selection to launch.
   * @param mode  The launching mode.
   * @return  The adapted source configuration.  {@code config}, or a working copy of it.
   * @throws CoreException 
   */
  protected ILaunchConfiguration adaptLaunchConfiguration(ILaunchConfiguration config, Selection selection, String mode) throws CoreException {
    return config;
  }
  
  /**
   * Get the launch configuration adapted to launch a selection.  The default implementation
   * proceeds as follows:
   * <ul>
   *   <li>It calls {@code findLaunchConfigurations} to find the candidate launch configurations,</li>
   *   <li>If there is exactly one candidate, it returns it (after adaptation),</li>
   *   <li>If there are more than one candidates, it shows a selection dialog to the user,</li>
   *   <li>If there is no candidate, it creates a new launch configuration and adapts it.</li>
   * </ul>
   * @param selection  The selection to launch.
   * @param mode  The launching mode.
   * @return  The launch configuration to launch, or null if the action has been canceled by the user.
   * @throws CoreException
   */
  protected ILaunchConfiguration getLaunchConfiguration(Selection selection, String mode) throws CoreException {
    List<ILaunchConfiguration> configs = findLaunchConfigurations(selection, mode);
    int candidateCount = configs.size();
    if (candidateCount == 0) return adaptLaunchConfiguration(createLaunchConfiguration(selection, mode), selection, mode);
    if (candidateCount == 1) return adaptLaunchConfiguration(configs.get(0), selection, mode);
    return adaptLaunchConfiguration(chooseConfiguration(configs), selection, mode);
  }
  
  /**
   * Returns a configuration from the given collection of configurations that should be launched,
   * or <code>null</code> to cancel. Default implementation opens a selection dialog that allows
   * the user to choose one of the specified launch configurations.  Returns the chosen configuration,
   * or <code>null</code> if the user cancels.
   * 
   * @param configs list of configurations to choose from
   * @return configuration to launch or <code>null</code> to cancel
   */
  protected ILaunchConfiguration chooseConfiguration(List<ILaunchConfiguration> configs) {
    IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
    ElementListSelectionDialog dialog= new ElementListSelectionDialog(MiscUi.getShell(), labelProvider);
    dialog.setElements(configs.toArray());
    dialog.setTitle(messages.getString("choose_configuration_title"));  
    dialog.setMessage(messages.getString("choose_configuration_message"));
    dialog.setMultipleSelection(false);
    int result = dialog.open();
    labelProvider.dispose();
    if (result == Window.OK) {
      return (ILaunchConfiguration) dialog.getFirstResult();
    }
    return null;        
  }


  // **************************************************************************
  // Implementation of ILaunchShortcut

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.jface.viewers.ISelection, java.lang.String)
   */
  @Override
  public void launch(ISelection selection, String mode) {
    ArrayList<IResource> resources = new ArrayList<IResource>();
    if (selection instanceof IStructuredSelection) {
      IStructuredSelection ssel = (IStructuredSelection) selection;
      @SuppressWarnings("unchecked")
      Iterator<Object> iterator = ssel.iterator();
      while (iterator.hasNext()) {
        Object obj = iterator.next();
        IResource resource = (IResource) Platform.getAdapterManager().getAdapter(obj, IResource.class);
        if (resource == null && obj instanceof IAdaptable) {
          resource = (IResource) ((IAdaptable) obj).getAdapter(IResource.class);
        }
        if (resource != null) { resources.add(resource); }
      }
    }
    launch(resources, mode);
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.ui.IEditorPart, java.lang.String)
   */
  @Override
  public void launch(IEditorPart editor, String mode) {
    IResource file = (IResource) editor.getEditorInput().getAdapter(IFile.class);
    if (file != null) launch(Collections.singleton(file), mode);
  }
  
  
  // **************************************************************************
  // Helper functions
  
  // See http://java2s.com/Open-Source/Java-Document/IDE-Eclipse/pde/org/eclipse/pde/ui/launcher/AbstractLaunchShortcut.java.htm

  /**
   * Helper function used to launch a configuration from a shortcut.
   * @param selection  The selection.
   * @param mode  The launch mode.
   * @throws CoreException 
   */
  private void launch(Iterable<IResource> selectionIterable, String mode) {
    try {
      Selection selection = getSelection(selectionIterable);
      if (selection == null) return;
      ILaunchConfiguration config = getLaunchConfiguration(selection, mode);
      if (config == null) return;
      config.launch(mode, null);
    } catch (CoreException exn) {
      IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, exn.getMessage(), exn.getCause());
      StatusManager.getManager().handle(status, StatusManager.SHOW);
    }
  }

}
