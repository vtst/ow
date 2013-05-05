package net.vtst.ow.eclipse.less.scoping;

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
  
  @Inject
  private IResourceScopeCache cache;
  
  @Inject
  private Provider<LoadOnDemandResourceDescriptions> loadOnDemandDescriptions;
  
  // **************************************************************************
  // Retrieving imported statements (for scope computation)
  
  /**
   * Compute the set of statements which are imported by an import statement.  This function manages recursive
   * import statements.  The result is cached to ensure efficiency.
   * @param importStatement  The import statement to resolve.
   * @return  The list of top-level statements of the imported stylesheets.
   */
  public Iterable<ToplevelStatement> getAllStatements(final ImportStatement importStatement) {
    return cache.get(Tuples.pair(LessImportStatementResolver.class, importStatement), importStatement.eResource(), new Provider<Iterable<ToplevelStatement>>() {
      public Iterable<ToplevelStatement> get() {
        LinkedList<ToplevelStatement> statements = new LinkedList<ToplevelStatement>();
        getAllStatementsRec(importStatement, statements, new HashSet<URI>());
        return statements;
      }
    });
  }
  
  /**
   * Recursive function to compute the list of imported statements.
   * @param importStatement  The import statement to resolve.
   * @param statements  The found statements are added to this list.
   * @param visitedURIs  The imported URIs are added to this set.  This is useful to avoid loops.
   */
  private void getAllStatementsRec(ImportStatement importStatement, List<ToplevelStatement> statements, Set<URI> visitedURIs) {
    ImportInfo importInfo = getImportInfo(importStatement);
    if (!importInfo.isLessFile() || !importInfo.isValid() || !visitedURIs.add(importInfo.uri)) return;
    StyleSheet styleSheet = importInfo.getImportedStyleSheet();
    if (styleSheet == null) return;
    for (ToplevelStatement toplevelStatement: styleSheet.getStatements()) {
      if (toplevelStatement instanceof ImportStatement) {
        getAllStatementsRec((ImportStatement) toplevelStatement, statements, visitedURIs); 
      } else {
        statements.add(toplevelStatement);
      }
    }
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
  
  private boolean checkImportStatementFormat(ImportStatement importStatement) {
    String format = importStatement.getFormat();
    return format == null || LessRuntimeModule.LESS_EXTENSION.equals(format) || LessRuntimeModule.CSS_EXTENSION.equals(format);
  }
  
  private List<ImportStatement> getImportStatements(StyleSheet styleSheet) {
    LinkedList<ImportStatement> list = new LinkedList<ImportStatement>();
    addImportStatements(list, styleSheet.getStatements());
    return list;
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
  
  public ImportInfo getImportInfo(ImportStatement importStatement) {
    return new ImportInfo(importStatement);
  }
  
  public class ImportInfo {
    public URI uri;
    public String format;
    private ImportStatement importStatement;
    
    ImportInfo(ImportStatement importStatement) {
      this.importStatement = importStatement;
      this.uri = URI.createURI(LessValueConverterService.getStringValue(importStatement.getUri()));
      this.format = importStatement.getFormat();
      if (this.format == null) {
        if (!URI_WITH_EXTENSION.matcher(uri.toString()).matches())
          uri = uri.appendFileExtension(LessRuntimeModule.LESS_EXTENSION);
        this.format = uri.fileExtension();
      }
    }
    
    public boolean isValid() {
      return !this.uri.isFile() || EcoreUtil2.isValidUri(this.importStatement, this.uri);     
    }
    
    public boolean isLessFile() {
      return LessRuntimeModule.LESS_EXTENSION.equals(this.format) && uri.isFile();
    }
    
    public Resource getResource() {
      return importStatement.eResource();
    }
    
    public StyleSheet getImportedStyleSheet() {
      if (this.isLessFile()) {
        LoadOnDemandResourceDescriptions resourceDescriptions = loadOnDemandDescriptions.get();
        resourceDescriptions.initialize(new IResourceDescriptions.NullImpl(), Collections.singleton(this.uri), this.getResource());
        IResourceDescription resourceDescription = resourceDescriptions.getResourceDescription(this.uri);
        for (IEObjectDescription objectDescription: resourceDescription.getExportedObjectsByType(LessPackage.eINSTANCE.getStyleSheet())) {
          EObject object = objectDescription.getEObjectOrProxy();
          if (object instanceof StyleSheet) return (StyleSheet) object;
        }
      }
      return null;
    }
  }
}
