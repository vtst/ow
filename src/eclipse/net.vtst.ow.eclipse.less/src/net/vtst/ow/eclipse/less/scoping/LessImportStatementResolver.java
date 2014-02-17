package net.vtst.ow.eclipse.less.scoping;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import net.vtst.ow.eclipse.less.LessRuntimeModule;
import net.vtst.ow.eclipse.less.less.Block;
import net.vtst.ow.eclipse.less.less.BlockUtils;
import net.vtst.ow.eclipse.less.less.ImportStatement;
import net.vtst.ow.eclipse.less.less.InnerRuleSet;
import net.vtst.ow.eclipse.less.less.LessPackage;
import net.vtst.ow.eclipse.less.less.StyleSheet;
import net.vtst.ow.eclipse.less.less.TerminatedMixin;
import net.vtst.ow.eclipse.less.less.ToplevelRuleSet;
import net.vtst.ow.eclipse.less.less.ToplevelStatement;
import net.vtst.ow.eclipse.less.parser.LessValueConverterService;
import net.vtst.ow.eclipse.less.properties.LessProjectProperty;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.scoping.impl.LoadOnDemandResourceDescriptions;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Tuples;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Helper class to resolve the import statements, and get the statements of the imported files.
 * @author Vincent Simonet
 */
public class LessImportStatementResolver {
  
  // The cache contains two kinds of entries:
  // - (ImportInfo.class, importStatement), for the import info of an import statement.
  // - (LessImportStatementResolver.class, stylesheet), for the set of import statements of
  //   a stylesheet.
  @Inject
  private IResourceScopeCache cache;
  
  @Inject
  private Provider<LoadOnDemandResourceDescriptions> loadOnDemandDescriptions;
  
  @Inject
  private LessProjectProperty projectProperty;
    
  // **************************************************************************
  // Retrieving imported statements (for scope computation)
  
  /**
   * Compute the set of statements which are imported by an import statement.
   * @param importStatement  The import statement to resolve.
   * @return  The list of top-level statements of the imported stylesheets.
   */
  public Iterable<ToplevelStatement> getImportedStatements(final ImportStatement importStatement) {
    ImportInfo importInfo = getImportInfo(importStatement);
    if (!importInfo.isLessFile() || !importInfo.isValid()) return Collections.emptyList();
    StyleSheet styleSheet = importInfo.getImportedStyleSheet();
    if (styleSheet == null) return Collections.emptyList();
    return styleSheet.getStatements();
  }
  
  // **************************************************************************
  // Validating imports

  public static enum ImportStatementCheckResult {
    OK,
    LOOP,
    INVALID_URI,
    INVALID_FORMAT
  }
  
  public ImportStatementCheckResult checkImportStatement(ImportStatement importStatement) {
    Resource resource = importStatement.eResource();
    ImportInfo importInfo = getImportInfo(importStatement);
    if (!importInfo.isValid()) return ImportStatementCheckResult.INVALID_URI;
    if (!checkImportStatementFormat(importStatement)) return ImportStatementCheckResult.INVALID_FORMAT;
    if (isImportLoop(resource, resource, importInfo, new HashSet<URI>()))
      return ImportStatementCheckResult.LOOP;
    else
      return ImportStatementCheckResult.OK;
  }
  
  private static String FORMAT_INLINE = "inline";
  private static String FORMAT_REFERENCE = "reference";
  
  private static Set<String> importFormats = new HashSet<String>(
      Arrays.asList(new String[]{LessRuntimeModule.LESS_EXTENSION, LessRuntimeModule.CSS_EXTENSION, FORMAT_INLINE, FORMAT_REFERENCE}));
  
  private boolean checkImportStatementFormat(ImportStatement importStatement) {
    String format = importStatement.getFormat();
    return format == null || importFormats.contains(format);
  }
  
  private Iterable<ImportStatement> getImportStatements(final StyleSheet styleSheet) {
    return cache.get(Tuples.pair(LessImportStatementResolver.class, styleSheet), styleSheet.eResource(), new Provider<Iterable<ImportStatement>>() {
      public Iterable<ImportStatement> get() {
        LinkedList<ImportStatement> list = new LinkedList<ImportStatement>();
        addImportStatements(list, styleSheet.getStatements());
        return list;
      }
    });
  }
  
  private void addImportStatements(List<ImportStatement> list, Iterable<? extends EObject> statements) {
    for (EObject statement : statements) {
      if (statement instanceof ImportStatement) {
        list.add((ImportStatement) statement);
      } else if (statement instanceof ToplevelRuleSet) {
        addImportStatements(list, ((ToplevelRuleSet) statement).getBlock());
      } else if (statement instanceof InnerRuleSet) {
        addImportStatements(list, ((InnerRuleSet) statement).getBlock());
      } else if (statement instanceof TerminatedMixin) {
        addImportStatements(list, ((TerminatedMixin) statement).getBody());        
      }
    }    
  }
  
  private void addImportStatements(List<ImportStatement> list, Block block) {
    if (block != null) addImportStatements(list, BlockUtils.iterator(block));
  }
  
  private boolean isImportLoop(Resource resource, Resource rootResource, ImportInfo importInfo, Set<URI> visitedURIs) {
    if (!importInfo.isLessFile() || !importInfo.isValid() || !visitedURIs.add(importInfo.uri)) return false;
    StyleSheet importedStyleSheet = importInfo.getImportedStyleSheet();
    if (importedStyleSheet == null) return false;
    if (importedStyleSheet.eResource().equals(rootResource)) return true;
    for (ImportStatement statement : getImportStatements(importedStyleSheet)) {
      if (isImportLoop(importedStyleSheet.eResource(), rootResource, getImportInfo(statement), visitedURIs))
        return true;      
    }
    return false;
  }
  
  // **************************************************************************
  // Getting imported style sheets

  private static Pattern URI_WITH_EXTENSION = Pattern.compile("(.*[.][a-z]*)|(.*[?;].*)");
  
  public ImportInfo getImportInfo(final ImportStatement importStatement) {
    return cache.get(Tuples.pair(ImportInfo.class, importStatement), importStatement.eResource(), new Provider<ImportInfo>() {
      public ImportInfo get() {
        return new ImportInfo(importStatement);
      }
    });
  }
    
  public class ImportInfo {
    public URI uri;
    public String format;
    private ImportStatement importStatement;
    
    ImportInfo(ImportStatement importStatement) {
      this.importStatement = importStatement;
      this.uri = resolveURI(LessValueConverterService.getStringValue(importStatement.getUri()));
      this.format = importStatement.getFormat();
      if (this.format == null) {
        if (!URI_WITH_EXTENSION.matcher(uri.toString()).matches())
          uri = uri.appendFileExtension(LessRuntimeModule.LESS_EXTENSION);
        this.format = uri.fileExtension();
      }
    }
    
    private IProject getProject(Resource resource) {
      URI uri = resource.getURI();
      IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(uri.toPlatformString(true)));
      if (file == null) return null;
      return file.getProject();
    }
    
    private URI resolveURI(String stringUri) {
      URI uri = URI.createURI(stringUri);
      if (uri.isFile()) {
        if (EcoreUtil2.isValidUri(this.importStatement, uri)) {
          return uri;
        } else if (uri.isRelative()) {
          // If the URI is relative, but not valid, try to resolve it with an include path.
          IProject project = getProject(this.importStatement.eResource());
          if (project != null) {
            for (IContainer container: projectProperty.getIncludePaths(project)) {
              if (container.exists()) {
                IFile file = container.getFile(new Path(stringUri));
                if (file.exists()) {
                  return URI.createFileURI(file.getLocation().toString());
                }
              }
            }
          }          
        }
        // If all above failed, this is not a valid URI.
        return null;
      } else {
        // Non-file URI are always considered as valid.
        return uri;
      }
    }
    
    public boolean isValid() {
      return this.uri != null;
    }
    
    public boolean isLessFile() {
      return (LessRuntimeModule.LESS_EXTENSION.equals(this.format) ||
          LessImportStatementResolver.FORMAT_REFERENCE.equals(this.format)) &&
          uri.isFile();
    }

    private IResourceDescription getResourceDescription() {
      LoadOnDemandResourceDescriptions resourceDescriptions = loadOnDemandDescriptions.get();
      resourceDescriptions.initialize(new IResourceDescriptions.NullImpl(), Collections.singleton(this.uri), this.importStatement.eResource());
      try {
        return resourceDescriptions.getResourceDescription(this.uri);
      } catch (IllegalStateException e) {
        // If the imported file does not have the expected content type, it is not controlled by XText, so we cannot load its
        // resource.
        return null;
      }      
    }
    
    private StyleSheet getImportedStyleSheetInternal() {
      if (this.isLessFile()) {
        IResourceDescription resourceDescription = getResourceDescription();
        if (resourceDescription == null) return null;
        for (IEObjectDescription objectDescription: resourceDescription.getExportedObjectsByType(LessPackage.eINSTANCE.getStyleSheet())) {
          EObject object = objectDescription.getEObjectOrProxy();
          if (object instanceof StyleSheet) return (StyleSheet) object;
        }
      }
      return null;
    }
    
    private boolean lazyStyleSheet = true;
    private StyleSheet styleSheet;
    
    public StyleSheet getImportedStyleSheet() {
      if (lazyStyleSheet) {
        lazyStyleSheet = false;
        styleSheet = getImportedStyleSheetInternal();
      }
      return styleSheet;
      
    }
  }
}
