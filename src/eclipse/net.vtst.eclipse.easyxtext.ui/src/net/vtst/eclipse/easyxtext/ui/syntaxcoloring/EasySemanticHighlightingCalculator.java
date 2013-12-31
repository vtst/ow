// EasyXtext
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.eclipse.easyxtext.ui.syntaxcoloring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.vtst.eclipse.easyxtext.guice.PostInject;
import net.vtst.eclipse.easyxtext.util.Pair;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.Action;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightedPositionAcceptor;
import org.eclipse.xtext.ui.editor.syntaxcoloring.ISemanticHighlightingCalculator;

/**
 * Abstract class for implementing a semantic highlighting calculator in a declarative way.
 * @author Vincent Simonet
 */
public abstract class EasySemanticHighlightingCalculator implements ISemanticHighlightingCalculator {

  private static class Attributes {
    public String defaultAttributeId = null;
    public List<Pair<Class<? extends EObject>, String>> semanticClassToAttributeId = null;
  }
  
  private static class AttributesMap<T> {
    private Map<T, Attributes> map = new HashMap<T, Attributes>();
    private Attributes getDefault(T key, boolean initList) {
      Attributes attributes = map.get(key);
      if (attributes == null) {
        attributes = new Attributes();
        map.put(key, attributes);
      }
      if (initList && attributes.semanticClassToAttributeId == null) {
        attributes.semanticClassToAttributeId = new ArrayList<Pair<Class<? extends EObject>, String>>();
      }
      return attributes;
    }
    public void add(T key, String attributeId) {
      Attributes attributes = getDefault(key, false);
      attributes.defaultAttributeId = attributeId;
    }
    public void add(T key, Class<? extends EObject> semanticClass, String attributeId) {
      Attributes data = getDefault(key, true);
      data.semanticClassToAttributeId.add(new Pair<Class<? extends EObject>, String>(semanticClass, attributeId));
    }
    public void provideHighlightingFor(T key, INode node, IHighlightedPositionAcceptor acceptor) {
      Attributes attributes = map.get(key);
      if (attributes == null) return;
      if (attributes.semanticClassToAttributeId != null) {
        Class<? extends EObject> semanticClass = node.getSemanticElement().getClass();
        for (Pair<Class<? extends EObject>, String> item: attributes.semanticClassToAttributeId) {
          if (item.getFirst().isAssignableFrom(semanticClass)) {
            acceptor.addPosition(node.getOffset(), node.getLength(), item.getSecond());
            return;
          }
        }
      }
      if (attributes.defaultAttributeId != null) {
        acceptor.addPosition(node.getOffset(), node.getLength(), attributes.defaultAttributeId);      
      }
    }
  }
  
  /**
   * Mapping from rule names to attribute IDs.
   */
  private AttributesMap<AbstractRule> ruleToAttributes = new AttributesMap<AbstractRule>();
  
  /**
   * Mapping from keywords to attribute IDs.
   */
  private AttributesMap<String> keywordsToAttributes = new AttributesMap<String>();
  
  /**
   * Mapping for actions.
   */
  private AttributesMap<Void> attributesForActions = new AttributesMap<Void>();
 
  
  /**
   * Bind a rule to an attribute.
   * @param rule
   * @param attribute
   */
  protected void bindRule(AbstractRule rule, EasyTextAttribute attribute) {
    bindRule(rule, attribute.getId());
  }
  
  /**
   * Bind a rule to an attribute.
   * @param rule
   * @param attributeId
   */
  protected void bindRule(AbstractRule rule, String attributeId) {
    ruleToAttributes.add(rule, attributeId);
  }

  /**
   * Bind a rule to an attribute, for occurrences appearing within a given semantic class.
   * @param rule
   * @param semanticClass
   * @param attribute
   */
  protected void bindRule(AbstractRule rule, Class<? extends EObject> semanticClass, EasyTextAttribute attribute) {
    bindRule(rule, semanticClass, attribute.getId());
  }
  
  /**
   * Bind a rule to an attribute, for occurrences appearing within a given semantic class.
   * @param rule
   * @param semanticClass
   * @param attributeId
   */
  protected void bindRule(AbstractRule rule, Class<? extends EObject> semanticClass, String attributeId) {
    ruleToAttributes.add(rule, semanticClass, attributeId);
  }
  
  
  /**
   * Bind a keyword to an attribute.
   * @param keyword
   * @param attribute
   */
  protected void bindKeyword(String keyword, EasyTextAttribute attribute) {
    bindKeyword(keyword, attribute.getId());
  }
  
  /**
   * Bind a keyword to an attribute.
   * @param keyword
   * @param attributeId
   */
  protected void bindKeyword(String keyword, String attributeId) {
    keywordsToAttributes.add(keyword, attributeId);
  }

  /**
   * Bind a keyword to an attribute, for occurrences appearing within a given semantic class.
   * @param keyword
   * @param semanticClass
   * @param attribute
   */
  protected void bindKeyword(String keyword, Class<? extends EObject> semanticClass, EasyTextAttribute attribute) {
    bindKeyword(keyword, semanticClass, attribute.getId());
  }
  
  /**
   * Bind a keyword to an attribute, for occurrences appearing within a given semantic class.
   * @param keyword
   * @param semanticClass
   * @param attributeId
   */
  protected void bindKeyword(String keyword, Class<? extends EObject> semanticClass, String attributeId) {
    keywordsToAttributes.add(keyword, semanticClass, attributeId);
  }

  /**
   * Bind a keyword to an attribute, for occurrences appearing within a given semantic class.
   * @param keyword
   * @param semanticClass
   * @param attribute
   */
  protected void bindAction(Class<? extends EObject> semanticClass, EasyTextAttribute attribute) {
    bindAction(semanticClass, attribute.getId());
  }
 
  /**
   * Bind an action to an attribute, for occurrences appearing within a given semantic class.
   * @param rule
   * @param semanticClass
   * @param attributeId
   */
  protected void bindAction(Class<? extends EObject> semanticClass, String attributeId) {
    attributesForActions.add(null, semanticClass, attributeId);
  }

    
  /**
   * Initialize the semantic highlighting calculator.  Sub-classes must implement this method,
   * and call the {@code bind*} methods to bind rules and keywords to attributes. 
   */
  protected abstract void configure();
  
  
  /**
   * This method is invoked automatically after dependency injection, and calls {@code configure}.
   */
  @PostInject
  public final void configurePostInject() {
    configure();
  }

  
  // **************************************************************************
  // Implementation of ISemanticHighlightingCalculator
  
  /**
   * Hook that sub-classes may use to implement special cases.
   * @param node
   * @param acceptor
   * @return
   */
  protected boolean provideCustomHighlightingFor(INode node, IHighlightedPositionAcceptor acceptor) {
    return false;
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.xtext.ui.editor.syntaxcoloring.ISemanticHighlightingCalculator#provideHighlightingFor(org.eclipse.xtext.resource.XtextResource, org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightedPositionAcceptor)
   */
  @Override
  public void provideHighlightingFor(XtextResource resource, IHighlightedPositionAcceptor acceptor) {
    if (resource == null || resource.getParseResult() == null) return;
    INode root = resource.getParseResult().getRootNode();
    for (INode node : root.getAsTreeIterable()) {
      if (!provideCustomHighlightingFor(node, acceptor)) {
        EObject grammarElement = node.getGrammarElement();
        if (grammarElement instanceof RuleCall) {
          ruleToAttributes.provideHighlightingFor(
              ((RuleCall) grammarElement).getRule(), node, acceptor);
        } else if (grammarElement instanceof Keyword) {
          keywordsToAttributes.provideHighlightingFor(
              ((Keyword) grammarElement).getValue(), node, acceptor);
        } else if (grammarElement instanceof Action) {
          attributesForActions.provideHighlightingFor(null, node, acceptor);
        }
        if (debug) printDebugInformation(node);
      }
    }
  }

  // **************************************************************************
  // Debugging stuff
  
  boolean debug = false;
  
  private void printDebugInformation(INode node) {
    EObject grammarElement = node.getGrammarElement();
    System.out.println("TEXT: " + node.getText());
    System.out.println("GRAMMAR: " + grammarElement.getClass().getName());
    if (node.getSemanticElement().eContainer() != null) System.out.println("SEMANTIC CONTAINER: " + node.getSemanticElement().eContainer().getClass().getName());
    if (node.getSemanticElement().eContainmentFeature() != null) System.out.println("SEMANTIC CONTAINMENT: " + node.getSemanticElement().eContainmentFeature().getName());
    System.out.println("SEMANTIC RESOURCE: " + node.getSemanticElement().eResource().getClass().getName());
    System.out.println("SEMANTIC: " + node.getSemanticElement().getClass().getName());
    if (grammarElement != null)
      System.out.println("GRAMMAR ELEMENT CLASS:" + grammarElement.getClass().getName());
    if (grammarElement instanceof RuleCall) {
      System.out.println("RULE: " + ((RuleCall) grammarElement).getRule().getName());
      System.out.println("CLASS: " + ((RuleCall) grammarElement).getRule().eClass().getName());
    } else if (grammarElement instanceof Action) {
      System.out.println("CARDINALITY: " + ((Action) grammarElement).getCardinality());
      System.out.println("FEATURE: " + ((Action) grammarElement).getFeature());
      System.out.println("OPERATOR: " + ((Action) grammarElement).getOperator());
    }
    System.out.println("");    
  }

}
