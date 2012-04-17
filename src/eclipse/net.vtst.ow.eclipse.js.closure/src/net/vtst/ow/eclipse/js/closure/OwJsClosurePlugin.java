package net.vtst.ow.eclipse.js.closure;

import java.net.URL;
import java.util.logging.Level;

import net.vtst.ow.eclipse.js.closure.builder.JavaScriptEditorRegistry;
import net.vtst.ow.eclipse.js.closure.builder.ProjectOrderManager;
import net.vtst.ow.eclipse.js.closure.compiler.IJSLibraryProvider;
import net.vtst.ow.eclipse.js.closure.launching.compiler.JSLibraryProviderForBuilder;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.google.javascript.jscomp.Compiler;

/**
 * The activator class controls the plug-in life cycle
 */
public class OwJsClosurePlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "net.vtst.ow.eclipse.js.closure"; //$NON-NLS-1$
	public static final String JS_CONTENT_TYPE_ID = "org.eclipse.wst.jsdt.core.jsSource";

	// The shared instance
	private static OwJsClosurePlugin plugin;
	private JavaScriptEditorRegistry editorRegistry;
  private OwJsClosureMessages messages;
  private JSLibraryProviderForBuilder jsLibraryProviderForClosureBuilder;
  private ProjectOrderManager projectOrderManager;
	
	/**
	 * The constructor
	 */
	public OwJsClosurePlugin() {
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
    editorRegistry = new JavaScriptEditorRegistry(getWorkbench());
    messages = new OwJsClosureMessages();
    jsLibraryProviderForClosureBuilder = new JSLibraryProviderForBuilder();
    projectOrderManager = new ProjectOrderManager();
    ResourcesPlugin.getWorkspace().addResourceChangeListener(projectOrderManager);  // TODO: Add int arg?
    Compiler.setLoggingLevel(Level.OFF);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		editorRegistry.dispose();
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(projectOrderManager);
		super.stop(context);
	}
	
	public JavaScriptEditorRegistry getEditorRegistry() {
	  return editorRegistry;
	}
	
	public IJSLibraryProvider getJSLibraryProviderForClosureBuilder() {
	  return jsLibraryProviderForClosureBuilder;
	}
	
	public ProjectOrderManager getProjectOrderManager() {
	  return projectOrderManager;
	}
	
	public OwJsClosureMessages getMessages() {
	  return messages;
	}
	
	protected IPath getImageBasePath() {
	  return new Path("icons");
	}
	
  public Image getImageFromRegistry(String key) {
    Image image = getImageRegistry().get(key);
    if (image == null) {
      image = createImage(key);
      getImageRegistry().put(key, image);
    }
    return image;
  }
  
  private Image createImage(String key) {
    IPath path = getImageBasePath().append(key);
    URL url= FileLocator.find(getBundle(), path, null);
    if (url != null) {
      ImageDescriptor descriptor = ImageDescriptor.createFromURL(url);
      if (descriptor != null) return descriptor.createImage();
    }
    return null;    
  }

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static OwJsClosurePlugin getDefault() {
		return plugin;
	}

}
