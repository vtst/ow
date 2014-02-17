package net.vtst.ow.eclipse.less.scoping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.vtst.ow.eclipse.less.LessMessages;
import net.vtst.ow.eclipse.less.LessRuntimeModule;
import net.vtst.ow.eclipse.less.less.Block;
import net.vtst.ow.eclipse.less.less.BlockUtils;
import net.vtst.ow.eclipse.less.less.ImportStatement;
import net.vtst.ow.eclipse.less.less.InnerRuleSet;
import net.vtst.ow.eclipse.less.less.LessPackage;
import net.vtst.ow.eclipse.less.less.StyleSheet;
import net.vtst.ow.eclipse.less.less.TerminatedMixin;
import net.vtst.ow.eclipse.less.less.ToplevelRuleSet;
import net.vtst.ow.eclipse.less.parser.LessValueConverterService;
import net.vtst.ow.eclipse.less.resource.LessResourceDescriptionStrategy;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.IContainer;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.scoping.impl.LoadOnDemandResourceDescriptions;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Tuples;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LessImportStatementResolver2 {

  // The cache contains:
  // - (ResolvedImportStatement.class, ImportStatement) => ResolvedImportStatement
  // - (LessImportStatementResolver2.class, StyleSheet) -> Iterable<ResolvedImportStatement>
  @Inject
  private IResourceScopeCache cache;

  @Inject
  private Provider<LoadOnDemandResourceDescriptions> loadOnDemandDescriptions;

  // **************************************************************************
  // Supported import formats
  
  private static String FORMAT_INLINE = "inline";
  private static String FORMAT_REFERENCE = "reference";
  
  private static Set<String> SUPPORTED_FORMATS = new HashSet<String>(
      Arrays.asList(new String[]{
          LessRuntimeModule.LESS_EXTENSION, LessRuntimeModule.CSS_EXTENSION, FORMAT_INLINE, FORMAT_REFERENCE}));
  
  // **************************************************************************
  // Containers

  @Inject
  private IContainer.Manager containerManager;
  
  @Inject
  private IResourceDescriptions resourceDescriptions;

  private IResourceDescription getResourceDescription(Resource resource, URI uri) {
    return resourceDescriptions.getResourceDescription(uri);
    // TODO: Do we want to manage containers?
    // TODO: How to manage URI which are relative to --include_paths?
//    IResourceDescription resourceDescription = resourceDescriptions.getResourceDescription(resource.getURI());
//    for (IContainer container : containerManager.getVisibleContainers(resourceDescription, resourceDescriptions)) {
//      IResourceDescription desc = container.getResourceDescription(uri);
//      if (desc != null) return desc;
//    }
//    return null;
  }

  // **************************************************************************
  // Class ResolvedImportStatement
  
  public ResolvedImportStatement resolve(final ImportStatement statement) {
    return cache.get(Tuples.pair(ResolvedImportStatement.class, statement), statement.eResource(), new Provider<ResolvedImportStatement>() {
      public ResolvedImportStatement get() {
        return new ResolvedImportStatement(statement);
      }
    });
  }

  @Inject
  private LessMessages messages;

  public enum ImportStatementErrorLevel { ERROR, WARNING }

  public class ImportStatementError {
    
    private ImportStatementErrorLevel level;
    private EStructuralFeature feature;
    private String messageKey;
    private String[] messageValues;

    public ImportStatementError(ImportStatementErrorLevel level, EStructuralFeature feature, String messageKey, String... messageValues) {
      this.level = level;
      this.feature = feature;
      this.messageKey = messageKey;
      this.messageValues = messageValues;
    }

    public void report(ImportStatement importStatement, ValidationMessageAcceptor acceptor) {
      switch (this.level) {
      case ERROR:
        acceptor.acceptError(messages.format(this.messageKey, this.messageValues), importStatement, this.feature, 0, null);
        break;
      case WARNING:
        acceptor.acceptWarning(messages.format(this.messageKey, this.messageValues), importStatement, this.feature, 0, null);
        break;
      }
    }
    
  }
    
  public class ResolvedImportStatement {
    
    private ImportStatement statement;
    private URI absoluteURI;
    private ImportStatementError error;
    private boolean isLocalAndLess;
    private StyleSheet importedStyleSheet;

    public ResolvedImportStatement(ImportStatement statement) {
      this.statement = statement;
      // Parse the URI argument.
      try {
        this.absoluteURI = createAbsoluteURI(LessValueConverterService.getStringValue(statement.getUri()), statement.eResource().getURI());
      } catch (IllegalArgumentException exn) {
        this.error = new ImportStatementError(ImportStatementErrorLevel.ERROR, LessPackage.eINSTANCE.getImportStatement_Uri(), "import_statement_error_illegal_uri", exn.getMessage());
        return;
      }
      // Determine the format.
      if (statement.getFormat() == null) {
        this.isLocalAndLess = LessRuntimeModule.LESS_EXTENSION.equals(this.absoluteURI.fileExtension()) && isLocalURI(this.absoluteURI);
      } else {
        if (SUPPORTED_FORMATS.contains(statement.getFormat())) {
          this.isLocalAndLess = LessRuntimeModule.LESS_EXTENSION.equals(statement.getFormat()) && isLocalURI(this.absoluteURI);
        } else {
          this.error = new ImportStatementError(ImportStatementErrorLevel.ERROR, LessPackage.eINSTANCE.getImportStatement_Format(), "import_statement_error_unknown_format", statement.getFormat());
          return;
        }
      }
      // Set the imported stylesheet.
      if (this.isLocalAndLess) {
        if (!this.setImportedStyleSheet()) {
          this.error = new ImportStatementError(ImportStatementErrorLevel.ERROR, LessPackage.eINSTANCE.getImportStatement_Uri(), "import_statement_error_file_not_found", statement.getFormat());
          return;
        }
      }
    }
    
    private boolean setImportedStyleSheet() {
      // TODO: Can we load directly?
      // IResourceDescription desc = getResourceDescription(this.statement.eResource(), this.absoluteURI);
      IResourceDescription desc = loadResourceDescription();
      if (desc != null) {
        for (IEObjectDescription objectDesc : desc.getExportedObjectsByType(LessPackage.eINSTANCE.getStyleSheet())) {
          if (LessResourceDescriptionStrategy.STYLESHEET_NAME.equals(objectDesc.getQualifiedName())) {
            EObject obj = objectDesc.getEObjectOrProxy();
            if (obj instanceof StyleSheet) {
              this.importedStyleSheet = (StyleSheet) obj;
              return true;
            }
          }
        }
      }
      return false;
    }
    
    private IResourceDescription loadResourceDescription() {
      LoadOnDemandResourceDescriptions lodrd = loadOnDemandDescriptions.get();
      lodrd.initialize(new IResourceDescriptions.NullImpl(), Collections.singleton(this.absoluteURI), this.statement.eResource());
      try {
        return lodrd.getResourceDescription(this.absoluteURI);
      } catch (IllegalStateException e) {
        // If the imported file does not have the expected content type, it is not controlled by XText, so we cannot load its
        // resource.
        return null;
      }      
    }

    
    private LazyImportCycleDetector cycleDetector = new LazyImportCycleDetector(this);
    public boolean isCycleRoot() { return cycleDetector.isCycleRoot(); }
    
    public ImportStatementError getError() { return this.error; }
    public URI getURI() { return this.absoluteURI; }
    public boolean hasError() { return this.error != null; }
    public boolean isLocalAndLess() { return this.isLocalAndLess; }
    public StyleSheet getImportedStyleSheet() { return this.importedStyleSheet; }
    
  }
  
  // **************************************************************************
  // Class ResolvedImportStatementsProvider
  
  private class ResolvedImportStatementsProvider implements Provider<Iterable<ResolvedImportStatement>> {
    
    private StyleSheet styleSheet;
    private List<ResolvedImportStatement> statements;
    
    private ResolvedImportStatementsProvider(StyleSheet styleSheet) {
      this.styleSheet = styleSheet;
    }
    
    private void visit(Block block) {
      visit(BlockUtils.iterator(block));
    }

    private void visit(Iterable<? extends EObject> statements) {
      for (EObject statement : statements) {
        if (statement instanceof ImportStatement) {
          this.statements.add(resolve((ImportStatement) statement));
        } else if (statement instanceof ToplevelRuleSet) {
          visit(((ToplevelRuleSet) statement).getBlock());
        } else if (statement instanceof InnerRuleSet) {
          visit(((InnerRuleSet) statement).getBlock());
        } else if (statement instanceof TerminatedMixin) {
          visit(((TerminatedMixin) statement).getBody());        
        }
      }
    }
    
    public Iterable<ResolvedImportStatement> get() { 
      this.statements = new ArrayList<ResolvedImportStatement>();
      try {
        visit(this.styleSheet.getStatements());
      } catch (Exception e) {
        e.printStackTrace();
      }
      return this.statements;
    }
  }
  
  private Iterable<ResolvedImportStatement> getResolvedImportStatements(StyleSheet styleSheet) {
    return cache.get(
        Tuples.pair(LessImportStatementResolver2.class, styleSheet),
        styleSheet.eResource(),
        new ResolvedImportStatementsProvider(styleSheet));
  }

  // **************************************************************************
  // Class LazyImportCycleDetector
  
  private class LazyImportCycleDetector {
    
    private ResolvedImportStatement statement;
    private Boolean isCycleRoot = null;
    private Set<URI> visitedURIs = null;

    private LazyImportCycleDetector(ResolvedImportStatement statement) {
      this.statement = statement;
    }
    
    private void visit(ResolvedImportStatement statement) {
      StyleSheet styleSheet = statement.getImportedStyleSheet();
      if (styleSheet != null) {
        for (ResolvedImportStatement statement2 : getResolvedImportStatements(styleSheet)) {
          if (this.visitedURIs.add(statement2.getURI())) visit(statement2);
        }
      }
    }
    
    private boolean isCycleRoot() {
      if (this.isCycleRoot == null) {
        this.visitedURIs = new HashSet<URI>();
        visit(this.statement);
        this.isCycleRoot = this.visitedURIs.contains(this.statement.getURI());
      }
      return this.isCycleRoot.booleanValue();
    }
    
  }

  // **************************************************************************
  // Helper functions
  
  private static URI createAbsoluteURI(String string, URI base) {
    URI uri = URI.createURI(string);
    if (uri.isRelative()) uri = uri.resolve(base);
    return uri;
  }
  
  private static Set<String> LOCAL_SCHEMES = new HashSet<String>(
      Arrays.asList(new String[]{"file", "platform"}));
  
  private static boolean isLocalURI(URI uri) {
    return uri.isRelative() || LOCAL_SCHEMES.contains(uri.scheme());
  }

}
