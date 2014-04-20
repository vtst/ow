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
import net.vtst.ow.eclipse.less.resource.ResourceDescriptionLoader;
import net.vtst.ow.eclipse.less.scoping.LessImportStatementResolver.ResolvedImportStatement;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.resource.IResourceDescription;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LessImportingStatementFinder implements IResourceChangeListener {

  @Inject
  private LessImportStatementResolver importStatementResolver;
  
  @Inject
  private LessProjectProperty projectProperty;
  
  @Inject
  private ResourceDescriptionLoader resourceDescriptionLoader;
  
  private Map<IProject, ProjectAdapter> projectAdapters = new HashMap<IProject, ProjectAdapter>();
  
  public LessImportingStatementFinder() {
    registerAsListener();
  }
  
  public ImportStatement getImportingStatement(Resource resource) {
    try {
      ResourceAdapter adapter = this.getOrCreateResourceAdapter(resource);
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

  private ProjectAdapter getOrCreateProjectAdapter(ResourceSet resourceSet, IProject project) {
    ProjectAdapter adapter = projectAdapters.get(project);
    if (adapter == null) {
      adapter = new ProjectAdapter(resourceSet, project);
      projectAdapters.put(project, adapter);
      adapter.update();
    }
    return adapter;
  }
  
  private class ProjectAdapter {

    private ResourceSet resourceSet;
    private IProject project;
    private boolean hasRoots;

    public ProjectAdapter(ResourceSet resourceSet, IProject project) {
      this.resourceSet = resourceSet;
      this.project = project;
    }
        
    private StyleSheet getStyleSheet(URI uri) {
      IResourceDescription resourceDescription = resourceDescriptionLoader.getResourceDescription(this.resourceSet, uri);
      if (resourceDescription == null) return null;
      return LessResourceDescriptionStrategy.getStyleSheet(resourceDescription);      
    }
    
    private void update() {
      this.hasRoots = false;
      for (IFile file : projectProperty.getRoots(project)) {
        this.hasRoots = true;
        StyleSheet styleSheet = getStyleSheet(URI.createURI(file.getLocationURI().toString()));
        if (styleSheet != null) getOrCreateResourceAdapter(styleSheet.eResource());
      }
    }
    
    private boolean hasRoots() { return this.hasRoots; }
    
  }

  // **************************************************************************
  // ResourceAdapter
    
  private ResourceAdapter getOrCreateResourceAdapter(Resource resource) {
    // TODO: It seems styleSheet.eResource() might be null!
    ResourceAdapter adapter = (ResourceAdapter) EcoreUtil.getAdapter(resource.eAdapters(), ResourceAdapter.class);
    if (adapter == null) {
      IProject project = LessProjectProperty.getProject(resource);
      ProjectAdapter projectAdapter = this.getOrCreateProjectAdapter(resource.getResourceSet(), project);
      // The adapter might have been created by the project adapter, so let's check again.
      adapter = (ResourceAdapter) EcoreUtil.getAdapter(resource.eAdapters(), ResourceAdapter.class);
      if (adapter == null) {
        adapter = new ResourceAdapter(projectAdapter, resource);
        resource.eAdapters().add(adapter);
        adapter.update();
      }
    }
    return adapter;
  }
  
  private class ResourceAdapter extends EContentAdapter {
    
    private ProjectAdapter projectAdapter;
    private List<ResourceAdapter> importedResources = new ArrayList<ResourceAdapter>();
    private Map<Resource, Integer> importingStatementCounts = new HashMap<Resource, Integer>();
    private ImportStatement importStatement;
    private Resource resource;
    
    private ResourceAdapter(ProjectAdapter projectAdapter, Resource resource) {
      this.projectAdapter = projectAdapter;
      this.resource = resource;
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
      this.importingStatementCounts.remove(resource);
    }
    
    private synchronized void addImportingStatement(Resource resource, ImportStatement statement) {
      Integer count = this.importingStatementCounts.get(resource);
      this.importingStatementCounts.put(resource, count == null ? 1 : count + 1);
      this.importStatement = statement;
      getImportingStatement();
    }
    
    private StyleSheet getStyleSheet() {
      for (EObject obj : this.resource.getContents()) {
        if (obj instanceof StyleSheet) return (StyleSheet) obj;
      }
      return null;
    }
    
    // TODO: What happen when a resource is deleted? Is it counter removed in all resources it is importing?
    private void update() {
      // Clear the importing statements
      for (ResourceAdapter adapter: this.importedResources) {
        adapter.removeImportingStatement(this.resource);
      }
      this.importedResources.clear();
      // Add the importing statement
      StyleSheet styleSheet = this.getStyleSheet();
      if (styleSheet != null) {
        for (ResolvedImportStatement ris : importStatementResolver.getResolvedImportStatements(styleSheet)) {
          if (!ris.hasError()) {
            // TODO: This might be null?
            ResourceAdapter adapter = getOrCreateResourceAdapter(ris.getImportedStyleSheet().eResource());
            importedResources.add(adapter);
            adapter.addImportingStatement(this.resource, ris.getStatement());
          }
        }
      }
    }
    
    private int getImportingStatementCountTotal() {
      int result = 0;
      for (Entry<?, Integer> entry: this.importingStatementCounts.entrySet()) {
        result += entry.getValue();
      }
      return result;
    }
    
    private ImportStatement getImportingStatement() {
      if (this.importStatement == null || getImportingStatementCountTotal() == 1) {
        return this.importStatement;
      } else {
        return null;
      }
    }

  }
  
}
