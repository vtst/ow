package net.vtst.ow.eclipse.less.scoping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.vtst.ow.eclipse.less.less.ImportStatement;
import net.vtst.ow.eclipse.less.less.StyleSheet;
import net.vtst.ow.eclipse.less.properties.LessProjectProperty;
import net.vtst.ow.eclipse.less.resource.LessResourceDescriptionStrategy;
import net.vtst.ow.eclipse.less.scoping.LessImportStatementResolver.ResolvedImportStatement;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LessImportingStatementFinder implements IResourceChangeListener {

  @Inject
  private LessImportStatementResolver importStatementResolver;
  
  @Inject
  private LessProjectProperty projectProperty;
  
  @Inject
  private IResourceDescriptions resourceDescriptions;
  
  private Map<IProject, ProjectAdapter> projectAdapters = new HashMap<IProject, ProjectAdapter>();
  
  public LessImportingStatementFinder() {
    registerAsListener();
  }
  
  public ImportStatement getImportingStatement(StyleSheet styleSheet) {
    try {
      ResourceAdapter adapter = this.getOrCreateResourceAdapter(styleSheet);
      if (adapter.getProjectAdapter().hasRoots()) {
        return adapter.getImportingStatement();
      } else {
        return null;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
      

  // **************************************************************************
  // IResourceChangeListener
  
  private void registerAsListener() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this, 
        IResourceChangeEvent.PRE_DELETE 
      | IResourceChangeEvent.POST_CHANGE 
      | IResourceChangeEvent.PRE_CLOSE
      );
  }
  
  public void unregisterAsListener() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
  }
  
  public void resourceChanged(IResourceChangeEvent event) {
    IResource resource = event.getResource();
    if (!(resource instanceof IProject)) return;
    IProject project = (IProject) resource;
    if (event.getType() == IResourceChangeEvent.PRE_CLOSE 
        || event.getType() == IResourceChangeEvent.PRE_DELETE) {
      this.projectAdapters.remove(project);
      return;
    } else {
      // event.getType() == IResourceChangeEvent.POST_CHANGE
      ProjectAdapter adapter = this.projectAdapters.get(project);
      if (adapter != null) adapter.update();
    }
  }
  
  
  // **************************************************************************
  // ProjectAdapter

  private ProjectAdapter getOrCreateProjectAdapter(Resource context, IProject project) {
    ProjectAdapter adapter = projectAdapters.get(project);
    if (adapter == null) {
      adapter = new ProjectAdapter(context, project);
      projectAdapters.put(project, adapter);
      adapter.update();
    }
    return adapter;
  }

  private class ProjectAdapter {

    // TODO: What to do if the context is deleted?
    private Resource context;
    private IProject project;
    private boolean hasRoots;

    public ProjectAdapter(Resource context, IProject project) {
      this.context = context;
      this.project = project;
    }
    
    private StyleSheet getStyleSheet(URI uri) {
      // TODO: This could be simplified.  As uri is absolute, we don't need context.
      IResourceDescription resourceDescription = importStatementResolver.loadResourceDescription(this.context, uri);
      // IResourceDescription resourceDescription = resourceDescriptions.getResourceDescription(uri);
      if (resourceDescription == null) return null;
      return LessResourceDescriptionStrategy.getStyleSheet(resourceDescription);      
    }
    
    private void update() {
      this.hasRoots = false;
      for (IFile file : projectProperty.getRoots(project)) {
        this.hasRoots = true;
        StyleSheet styleSheet = getStyleSheet(URI.createURI(file.getLocationURI().toString()));
        if (styleSheet != null) getOrCreateResourceAdapter(styleSheet);
      }
    }
    
    private boolean hasRoots() { return this.hasRoots; }
    
  }

  // **************************************************************************
  // ResourceAdapter

  private ResourceAdapter getOrCreateResourceAdapter(StyleSheet styleSheet) {
    ResourceAdapter adapter = (ResourceAdapter) EcoreUtil.getAdapter(styleSheet.eResource().eAdapters(), ResourceAdapter.class);
    if (adapter == null) {
      IProject project = LessProjectProperty.getProject(styleSheet.eResource());
      ProjectAdapter projectAdapter = this.getOrCreateProjectAdapter(styleSheet.eResource(), project);
      adapter = new ResourceAdapter(projectAdapter, styleSheet);
      styleSheet.eResource().eAdapters().add(adapter);
      adapter.update();
    }
    return adapter;
  }

  private class ResourceAdapter extends EContentAdapter {
    
    private ProjectAdapter projectAdapter;
    private StyleSheet styleSheet;
    private List<ResourceAdapter> importedResources = new ArrayList<ResourceAdapter>();
    private Map<Resource, ImportStatement> importingStatements = new HashMap<Resource, ImportStatement>();
    
    private ResourceAdapter(ProjectAdapter projectAdapter, StyleSheet styleSheet) {
      this.projectAdapter = projectAdapter;
      this.styleSheet = styleSheet;
    }
    
    private ProjectAdapter getProjectAdapter() {
      return this.projectAdapter;
    }
    
    @Override
    public void notifyChanged(Notification notification) {
      super.notifyChanged(notification);
      if (this.isSemanticStateChange(notification)) {
        update();
      }
    }

    private boolean isSemanticStateChange(Notification notification) {
      return !notification.isTouch() &&
          !(notification.getNewValue() instanceof Diagnostic) &&
          !(notification.getOldValue() instanceof Diagnostic);
    }

    @Override
    public boolean isAdapterForType(Object type) {
      return type == getClass();
    }

    @Override
    protected boolean resolve() {
      return false;
    }
    
    private void removeImportingStatement(Resource resource) {
      this.importingStatements.remove(resource);
    }
    
    private void addImportingStatement(Resource resource, ImportStatement statement) {
      if (this.importingStatements.put(resource, statement) != null) {
        // TODO: Do this handle properly the case of multiple import statements from the same or different resources?
        this.importingStatements.put(resource, null);
      }
    }
    
    private void update() {
      // Clear the importing statements
      for (ResourceAdapter adapter: this.importedResources) {
        adapter.removeImportingStatement(this.styleSheet.eResource());
      }
      this.importedResources.clear();
      // Add the importing statement
      for (ResolvedImportStatement ris : importStatementResolver.getResolvedImportStatements(styleSheet)) {
        if (!ris.hasError()) {
          ResourceAdapter adapter = getOrCreateResourceAdapter(ris.getImportedStyleSheet());
          importedResources.add(adapter);
          adapter.addImportingStatement(this.styleSheet.eResource(), ris.getStatement());
        }
      }
    }
    
    private ImportStatement getImportingStatement() {
      if (this.importingStatements.size() == 1) {
        for (Entry<Resource, ImportStatement> entry: this.importingStatements.entrySet())
          return entry.getValue();
      }
      return null;
    }

  }
  
}
