package net.vtst.ow.eclipse.less.scoping;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.vtst.ow.eclipse.less.LessRuntimeModule;
import net.vtst.ow.eclipse.less.less.ImportStatement;
import net.vtst.ow.eclipse.less.less.LessPackage;
import net.vtst.ow.eclipse.less.less.StyleSheet;
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
    URI uri = getURI(importStatement);
    if (uri == null || !visitedURIs.add(uri)) return;
    StyleSheet styleSheet = getImportedStyleSheet(importStatement);
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
    INVALID_URI
  }
  
  public ImportStatementCheckResult checkImportStatement(ImportStatement importStatement) {
    Resource resource = importStatement.eResource();
    URI importUri = getURI(importStatement);
    if (importUri == null) return ImportStatementCheckResult.INVALID_URI;
    if (isImportLoop(resource, resource, importUri, new HashSet<URI>()))
      return ImportStatementCheckResult.LOOP;
    else
      return ImportStatementCheckResult.OK;
  }
  
  private boolean isImportLoop(Resource resource, Resource rootResource, URI importUri, Set<URI> visitedURIs) {
    if (importUri == null || !visitedURIs.add(importUri)) return false;
    StyleSheet importedStyleSheet = getImportedStyleSheet(resource, importUri);
    if (importedStyleSheet == null) return false;
    if (importedStyleSheet.eResource().equals(rootResource)) return true;
    for (ToplevelStatement statement: importedStyleSheet.getStatements()) {
      if (statement instanceof ImportStatement) {
        if (isImportLoop(importedStyleSheet.eResource(), rootResource, getURI((ImportStatement) statement), visitedURIs))
          return true;
      }
    }
    return false;
  }
  
  // **************************************************************************
  // Getting imported style sheets

  /**
   * Converts an import statement into an URI.
   * @param importStatement  The import statement to convert.
   * @return  The imported URI, or {@code null} if the URI of the import statement is not valid.
   */
  public URI getURI(ImportStatement importStatement) {
    URI uri = URI.createURI(LessValueConverterService.getStringValue(importStatement.getUri()));
    String fileExtension = uri.fileExtension();
    if (fileExtension == null) uri = uri.appendFileExtension(LessRuntimeModule.LESS_EXTENSION);
    if (!uri.isFile() || EcoreUtil2.isValidUri(importStatement, uri)) {
      return uri;
    } else {
      return null;
   }
  }

  /**
   * @param uri
   * @return true iif uri is the URI of a LESS StyleSheet.
   */
  private boolean isLessStyleSheetURI(URI uri) {
    return uri.isFile() && LessRuntimeModule.LESS_EXTENSION.equals(uri.fileExtension());
  }
  
  /**
   * Get an imported style sheet.
   * @param resource  The resource which is importing. 
   * @param uri  The URI of the style sheet to import.
   * @return  The imported style sheet, or {@code null}.
   */
  private StyleSheet getImportedStyleSheet(Resource resource, URI uri) {
    if (!isLessStyleSheetURI(uri)) return null;
    LoadOnDemandResourceDescriptions resourceDescriptions = loadOnDemandDescriptions.get();
    resourceDescriptions.initialize(new IResourceDescriptions.NullImpl(), Collections.singleton(uri), resource);
    IResourceDescription resourceDescription = resourceDescriptions.getResourceDescription(uri);
    for (IEObjectDescription objectDescription: resourceDescription.getExportedObjectsByType(LessPackage.eINSTANCE.getStyleSheet())) {
      EObject object = objectDescription.getEObjectOrProxy();
      if (object instanceof StyleSheet) return (StyleSheet) object;
    }
    return null;
  }
  
  /**
   * Get the style sheet imported by an import statement.
   * @param importStatement  The import statement.
   * @return The style sheet, or {@code null}.
   */
  private StyleSheet getImportedStyleSheet(ImportStatement importStatement) {
    return getImportedStyleSheet(importStatement.eResource(), getURI(importStatement));
  }
}
