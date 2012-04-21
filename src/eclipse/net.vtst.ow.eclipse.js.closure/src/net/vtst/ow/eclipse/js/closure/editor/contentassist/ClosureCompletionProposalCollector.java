package net.vtst.ow.eclipse.js.closure.editor.contentassist;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.javascript.jscomp.Scope;
import com.google.javascript.jscomp.Scope.Var;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.JSDocInfo.Visibility;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.jstype.JSType;
import com.google.javascript.rhino.jstype.ObjectType;
import com.google.javascript.rhino.jstype.UnionType;

/**
 * Helper class for creating and collecting closure completion proposals.
 * @author Vincent Simonet
 */
public class ClosureCompletionProposalCollector {

  private static String THIS = "this";

  private ClosureContentAssistIncovationContext context;
  
  public ClosureCompletionProposalCollector(ClosureContentAssistIncovationContext context) {
    this.context = context;
  }
    
  /**
   * Get all the completion proposals which are valid in the given context.
   * @return  The list of the completion proposals.
   */
  public List<ClosureCompletionProposal> getProposals() {
    String[] segments = context.getPath();
    if (segments.length > 0) {
      JSType type = getTypeOfPath(segments);
      if (type != null) {
        return getProposalsFromType(type);    
      }
    } else {
      try {
        return getProposalsFromScope();
      } catch (RuntimeException e) {}
    } 
    return Collections.emptyList();
  }

  // **************************************************************************
  // Getting completion proposals from variables in the scope
  
  
  /**
   * Get the list of completion proposals computed from the scope.  This method is called
   * in the case where the prefix does not contain a dot.
   * @return  The list of completion proposals.
   */
  private List<ClosureCompletionProposal> getProposalsFromScope() {
    String prefix = context.getPrefix();
    LinkedList<ClosureCompletionProposal> list = new LinkedList<ClosureCompletionProposal>();
    for (Var var: context.getCompilerRun().getAllSymbols(context.getNode())) {
      if (isValidFor(var.getName(), prefix) && isSimpleName(var.getName())) {
        Node node = var.getNameNode();
        String name = var.getName();
        if (isConcreteNode(node) && isVisibleName(name)) {
          list.add(new ClosureCompletionProposal(
              context, name, node, var.getType(), var.getJSDocInfo(),
              false, var.isLocal()));
        }
      }
    }
    return list;
  }
  
  // **************************************************************************
  // Getting completion proposals from properties
    
  /**
   * Get the type of the prefix in the context.  For instance, if the prefix is
   * foo.bar.x, this method will return the type of foo.bar (or null if it cannot
   * be determined).
   * @param segments  The segments which constitute the prefix
   * @return  The type, or null if no type can be found in the context.
   */
  private JSType getTypeOfPath(String[] segments) {
    assert segments.length > 0;
    Scope scope = context.getCompilerRun().getScope(context.getNode());
    if (scope != null) {
      JSType type = null;
      if (THIS.equals(segments[0])) {
        type = scope.getTypeOfThis();
      } else {
        Var var = scope.getVar(segments[0]);
        if (var != null) type = var.getType();
      }
      for (int i = 1; i < segments.length; ++i) {
        if (type == null) break;
        type = type.findPropertyType(segments[i]);
      }
      return type;
    }
    return null;
  }

  /**
   * Compute the list of completion proposals for a given type and last segment.
   * This method is used in the case where the prefix is a qualified name.
   * @param type  The type to look at for building completion proposals.
   * @return  The list of completion proposals.
   */
  private List<ClosureCompletionProposal> getProposalsFromType(JSType type) {
    Map<String, ClosureCompletionProposal> map = new HashMap<String, ClosureCompletionProposal>();
    if (type instanceof UnionType) {
      for (JSType alternateType: ((UnionType) type).getAlternates()) {
        collectProposalsFromType(map, context.getPrefix(), alternateType);
      }
    } else {
      collectProposalsFromType(map, context.getPrefix(), type);
    }
    return Lists.newArrayList(map.values());
  }

  
  /**
   * Get the doc info for a property in an object type, by walking through the type hierarchy.
   * @param objectType  The objectType to which the property belong to.
   * @param propertyName  The name of the property.
   * @return  The doc info, or null if not found.
   */
  public static JSDocInfo getJSDocInfoOfProperty(ObjectType objectType, String propertyName) {
    for (; objectType != null;
        objectType = objectType.getImplicitPrototype()) {
      JSDocInfo docInfo = objectType.getOwnPropertyJSDocInfo(propertyName);
      if (docInfo != null) return docInfo;
    }
    return null;    
  }
  
  /**
   * Get the visibility of a property for a given object type, by walking through the type hierarchy.
   * @param objectType  The object type to inspect.
   * @param propertyName  The name of the property.
   * @return  The visibility of the property for the type.  PUBLIC if not specified.
   */
  private static Visibility getVisibilityOfProperty(ObjectType objectType, String propertyName) {
    for (; objectType != null;
        objectType = objectType.getImplicitPrototype()) {
      JSDocInfo docInfo = objectType.getOwnPropertyJSDocInfo(propertyName);
      if (docInfo != null &&
          docInfo.getVisibility() != Visibility.INHERITED) {
        return docInfo.getVisibility();
      }
    }
    return Visibility.PUBLIC;
  }

  /**
   * Collect the completion proposal for a type.
   * @param map  The map in which completion proposals are inserted.
   * @param prefix  The last segment of the prefix.
   * @param alternateType  The type to analyze.
   * @param type  The union type which {@code alternateType} belongs to.
   */
  private void collectProposalsFromType(
      Map<String, ClosureCompletionProposal> map,
      String prefix, JSType alternateType) {
    if (alternateType instanceof ObjectType) {
      ObjectType alternateObjectType = (ObjectType) alternateType;
      for (String propertyName: alternateObjectType.getPropertyNames()) {
        if (isValidFor(propertyName, prefix)) {
          ClosureCompletionProposal proposal = map.get(propertyName);
          if (proposal == null) {
            Node node = alternateObjectType.getPropertyNode(propertyName);
            if (isConcreteNode(node) && isVisibleName(propertyName)) {
              JSDocInfo docInfo = getJSDocInfoOfProperty(alternateObjectType, propertyName);
              JSType propertyType = alternateObjectType.getPropertyType(propertyName);
              proposal = new ClosureCompletionProposal(
                  context, propertyName, node, propertyType, docInfo,
                  true, false);
              map.put(propertyName, proposal);
            }
          }
          if (proposal != null) {
            proposal.addVisibility(getVisibilityOfProperty(alternateObjectType, propertyName));
          }
        }
      }
    }    
  }
  
  // **************************************************************************
  // Predicates 

  /**
   * Tests whether a name is a simple name (v.s. a qualified name).
   * @param name  The name to test.
   * @return  true if {@code name} is a simple name, i.e. does not contain any dot.
   */
  private boolean isSimpleName(String name) {
    return name.indexOf('.') < 0;
  }

  /**
   * Tests whether a node corresponds to a node of the source code.  Completion
   * proposals should not be created for non-concrete nodes.
   * @param node  The node to test.
   * @return  true if {@code node} is a concrete node.
   */
  private boolean isConcreteNode(Node node) {
    return node != null && !node.isSyntheticBlock() && node.getLineno() >= 0;
  }
  
  private boolean isVisibleName(String name) {
    int n = name.length();
    if (n < 4) return true;
    return name.charAt(0) != '_' || name.charAt(1) != '_' || 
        name.charAt(n - 1) != '_' || name.charAt(n - 2) != '_';
  }

  /**
   * Tests whether a name is valid for a completion proposal.
   * @param name  The name of the completion proposal.
   * @param prefix  The last segment of the context
   * @return true if {@code name} is a valid completion proposal name.
   */
  private boolean isValidFor(String name, String prefix) {
    return name.startsWith(prefix);
  }
}
