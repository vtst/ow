package net.vtst.ow.eclipse.js.closure.editor.contentassist;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.vtst.ow.closure.compiler.compile.CompilerRun;
import net.vtst.ow.eclipse.js.closure.editor.JSElementInfo;

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

  private ClosureContentAssistIncovationContext context;
  
  public ClosureCompletionProposalCollector(ClosureContentAssistIncovationContext context) {
    this.context = context;
  }
    
  /**
   * Get all the completion proposals which are valid in the given context.
   * @return  The list of the completion proposals.
   */
  public List<ClosureCompletionProposal> getProposals() {
    CompilerRun run = context.getCompilerRun();
    List<String> qualifiedName = context.getPrefixAsQualifiedName();
    if (qualifiedName.isEmpty()) {
      try {
        return getProposalsFromScope();
      } catch (RuntimeException e) {
        return Collections.emptyList();
      }      
    } else {
      Scope scope = run.getScope(context.getNode());
      if (scope == null) return Collections.emptyList();
      JSType type = CompilerRun.getTypeOfQualifiedName(scope, qualifiedName);
      if (type == null) return Collections.emptyList();
      else return getProposalsFromType(type);
    }
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
      String name = var.getName();
      if (isValidForPrefix(name, prefix) && isSimpleName(name) &&
          isVisibleName(name) && isConcreteNode(var.getNameNode())) {
        list.add(new ClosureCompletionProposal(
            context, name, JSElementInfo.makeFromVar(context.getCompilerRun(), var)));
      }
    }
    return list;
  }
  
  // **************************************************************************
  // Getting completion proposals from properties

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
        if (isValidForPrefix(propertyName, prefix) && isVisibleName(propertyName)) {
          ClosureCompletionProposal proposal = map.get(propertyName);
          if (proposal == null) {
            // This is slightly unefficient, because we build the elementInfo before
            // checking that the node is concrete.  But I'm not sure it is useful to
            // complicate the code for this particular case.
            JSElementInfo elementInfo = JSElementInfo.makeFromProperty(context.getCompilerRun(), alternateObjectType, propertyName);
            if (isConcreteNode(elementInfo.getNode())) {
              proposal = new ClosureCompletionProposal(context, propertyName, elementInfo);
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
  
  /**
   * Tests whether a property name should be shown as a completion proposal.
   * @param name  A property name.
   * @return  true if {@code name} should be shown as a completion proposal.
   */
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
  private boolean isValidForPrefix(String name, String prefix) {
    // Test if name starts by prefix, ignoring case.
    // This check must be consistent with the one implemented in 
    // AbstractCompletionProposal.validate
    return name.regionMatches(true, 0, prefix, 0, prefix.length());
  }
}
