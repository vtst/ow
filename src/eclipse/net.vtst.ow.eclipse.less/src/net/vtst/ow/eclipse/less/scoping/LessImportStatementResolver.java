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
import net.vtst.ow.eclipse.less.less.ImportStatement;
import net.vtst.ow.eclipse.less.less.InnerRuleSet;
import net.vtst.ow.eclipse.less.less.LessPackage;
import net.vtst.ow.eclipse.less.less.Mixin;
import net.vtst.ow.eclipse.less.less.StyleSheet;
import net.vtst.ow.eclipse.less.less.ToplevelRuleSet;
import net.vtst.ow.eclipse.less.parser.LessValueConverterService;
import net.vtst.ow.eclipse.less.properties.LessProjectProperty;
import net.vtst.ow.eclipse.less.resource.LessResourceDescriptionStrategy;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.scoping.impl.LoadOnDemandResourceDescriptions;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Tuples;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LessImportStatementResolver {

  // The cache contains:
  // - (ResolvedImportStatement.class, ImportStatement) => ResolvedImportStatement
  // - (LessImportStatementResolver2.class, StyleSheet) -> Iterable<ResolvedImportStatement>
  @Inject
  private IResourceScopeCache cache;

  @Inject
  private Provider<LoadOnDemandResourceDescriptions> loadOnDemandDescriptions;
  
  @Inject
  private LessProjectProperty projectProperty;

  // **************************************************************************
  // Supported import formats
  
  private static String FORMAT_INLINE = "inline";
  private static String FORMAT_REFERENCE = "reference";
  
  private static Set<String> SUPPORTED_FORMATS = new HashSet<String>(
      Arrays.asList(new String[]{
          LessRuntimeModule.LESS_EXTENSION, LessRuntimeModule.CSS_EXTENSION, FORMAT_INLINE, FORMAT_REFERENCE}));
  
  // **************************************************************************
  // Containers

  private IResourceDescription loadResourceDescription(Resource resource, URI uri) {
    LoadOnDemandResourceDescriptions lodrd = loadOnDemandDescriptions.get();
    lodrd.initialize(new IResourceDescriptions.NullImpl(), Collections.<URI>emptyList(), resource);
    try {
      IResourceDescription rd = lodrd.getResourceDescription(uri);
      return rd;
    } catch (IllegalStateException e) {
      // If the imported file does not have the expected content type, it is not controlled by XText, so we cannot load its
      // resource.
      return null;
    }      
  }
  
  private IResourceDescription getResourceDescription(Resource resource, URI uri) {
    IResourceDescription resourceDescription = loadResourceDescription(resource, uri);
    if (resourceDescription == null && uri.isRelative()) {
      IProject project = LessProjectProperty.getProject(resource);
      if (project != null) {
        for (IContainer container : projectProperty.getIncludePaths(project)) {
          resourceDescription = loadResourceDescription(resource, uri.resolve(URI.createURI(container.getLocationURI().toString() + "/")));
          if (resourceDescription != null) break;
        }
      }
    }
    return resourceDescription;
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
    private URI uri;
    private ImportStatementError error;
    private boolean isLocalAndLess;
    private StyleSheet importedStyleSheet;

    public ResolvedImportStatement(ImportStatement statement) {
      this.statement = statement;
      // Parse the URI argument.
      try {
        this.uri = createURI(LessValueConverterService.getStringValue(statement.getUri()));
      } catch (IllegalArgumentException exn) {
        this.error = new ImportStatementError(ImportStatementErrorLevel.ERROR, LessPackage.eINSTANCE.getImportStatement_Uri(), "import_statement_error_illegal_uri", exn.getMessage());
        return;
      }
      // Determine the format.
      if (statement.getFormat() == null) {
        this.isLocalAndLess = LessRuntimeModule.LESS_EXTENSION.equals(this.uri.fileExtension()) && isLocalURI(this.uri);
      } else {
        if (SUPPORTED_FORMATS.contains(statement.getFormat())) {
          this.isLocalAndLess = LessRuntimeModule.LESS_EXTENSION.equals(statement.getFormat()) && isLocalURI(this.uri);
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
      IResourceDescription desc = getResourceDescription(this.statement.eResource(), this.uri);
      if (desc != null) {
        this.importedStyleSheet = LessResourceDescriptionStrategy.getStyleSheet(desc);
        if (this.importedStyleSheet != null) return true;
      }
      return false;
    }
    
    private LazyImportCycleDetector cycleDetector = null;
    private void checkCycle() {
      if (this.cycleDetector == null) {
        this.cycleDetector = new LazyImportCycleDetector(this);
        if (this.cycleDetector.isCycleRoot() && this.error == null) {
          this.error = new ImportStatementError(ImportStatementErrorLevel.ERROR, LessPackage.eINSTANCE.getImportStatement_Uri(), "import_loop");
        }
      }
    }
    
    public ImportStatementError getError() { checkCycle(); return this.error; }
    public boolean hasError() { checkCycle(); return this.error != null; }
    public URI getURI() { return this.uri; }
    public boolean isLocalAndLess() { return this.isLocalAndLess; }
    public StyleSheet getImportedStyleSheet() { return this.importedStyleSheet; }
    public ImportStatement getStatement() { return this.statement; }
    
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
      if (block != null) visit(block.getContent().getStatement());
    }

    private void visit(Iterable<? extends EObject> statements) {
      for (EObject statement : statements) {
        if (statement instanceof ImportStatement) {
          this.statements.add(resolve((ImportStatement) statement));
        } else if (statement instanceof ToplevelRuleSet) {
          visit(((ToplevelRuleSet) statement).getBlock());
        } else if (statement instanceof InnerRuleSet) {
          visit(((InnerRuleSet) statement).getBlock());
        } else if (statement instanceof Mixin) {
          visit(((Mixin) statement).getBody());        
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
  
  public Iterable<ResolvedImportStatement> getResolvedImportStatements(StyleSheet styleSheet) {
    return cache.get(
        Tuples.pair(LessImportStatementResolver.class, styleSheet),
        styleSheet.eResource(),
        new ResolvedImportStatementsProvider(styleSheet));
  }

  // **************************************************************************
  // Class LazyImportCycleDetector
  
  private class LazyImportCycleDetector {
    
    private boolean isCycleRoot;
    private Set<URI> visitedURIs = new HashSet<URI>();

    private LazyImportCycleDetector(ResolvedImportStatement statement) {
      visit(statement);
      this.isCycleRoot = this.visitedURIs.contains(statement.getURI());
    }
    
    private void visit(ResolvedImportStatement statement) {
      StyleSheet styleSheet = statement.getImportedStyleSheet();
      if (styleSheet != null) {
        for (ResolvedImportStatement statement2 : getResolvedImportStatements(styleSheet)) {
          if (this.visitedURIs.add(statement2.getURI())) visit(statement2);
        }
      }
    }
    
    private boolean isCycleRoot() { return this.isCycleRoot; }
    
  }

  // **************************************************************************
  // Helper functions
  
  private static URI createURI(String string) {
    if (string.startsWith("/"))
      return URI.createFileURI(string);
    else
      return URI.createURI(string);
  }

  private static Set<String> LOCAL_SCHEMES = new HashSet<String>(
      Arrays.asList(new String[]{"file", "platform"}));
  
  private static boolean isLocalURI(URI uri) {
    return uri.isRelative() || LOCAL_SCHEMES.contains(uri.scheme());
  }

}
