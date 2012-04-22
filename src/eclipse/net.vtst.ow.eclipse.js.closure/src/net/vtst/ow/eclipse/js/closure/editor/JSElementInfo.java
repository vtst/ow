package net.vtst.ow.eclipse.js.closure.editor;

import java.util.Set;

import net.vtst.ow.closure.compiler.compile.CompilerRun;
import net.vtst.ow.eclipse.js.closure.OwJsClosureImages;
import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.editor.contentassist.IAdditionalProposalInfoProvider;
import net.vtst.ow.eclipse.js.closure.util.HTMLPrinter;
import net.vtst.ow.eclipse.js.closure.util.Utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.javascript.jscomp.NodeUtil;
import com.google.javascript.jscomp.Scope.Var;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.JSDocInfo.Visibility;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;
import com.google.javascript.rhino.jstype.FunctionType;
import com.google.javascript.rhino.jstype.JSType;
import com.google.javascript.rhino.jstype.ObjectType;

/**
 * Class for storing the information about a JS element.  It provides some logic which is
 * common to completion proposals and hovers.  
 * It implements {@code IAdditionalProposalInfo}, so that it can be passed by a 
 * completion proposal as an additional proposal info for display when a completion
 * proposal is selected.
 * @author Vincent Simonet
 */
public class JSElementInfo implements IAdditionalProposalInfoProvider {
  
  private OwJsClosureMessages messages;
  private CompilerRun run;
  
  // Input properties
  private Node node;
  private JSDocInfo docInfo;
  private JSType type;
  private boolean isProperty;
  private boolean isLocalVariable;
  private Visibility visibility = Visibility.PUBLIC;
  
  // Computed properties
  private JSElementKind kind;
  private boolean isNamespace;
  
  // HTML generation
  private StringBuffer buf;
  private String htmlString;

  // **************************************************************************
  // Constructor and factories

  /**
   * Create a new additional proposal info.
   * @param node  The node to which the proposal info is relative.
   * @param docInfo  The doc info to use for filling the proposal info.
   * @param type  The type to use for filling the proposal info.
   */
  private JSElementInfo(
      CompilerRun run, Node node, JSType type, JSDocInfo docInfo,
      boolean isProperty, boolean isLocalVariable) {
    this.run = run;
    this.node = node;
    this.type = type;
    this.docInfo = docInfo;
    this.isProperty = isProperty;
    this.isLocalVariable = isLocalVariable;
    this.isNamespace = isNamespace(node);
    this.kind = computeKind();
  }
  
  /**
   * Creates a new {@code JSElementInfo} from a top-level variable.
   * @param run  The compiler run (to be used to retrieve further information).
   * @param var  The top-level variable.
   * @return  A new {@code JSElementInfo}.
   */
  public static JSElementInfo makeFromVar(CompilerRun run, Var var) {
    return new JSElementInfo(run, var.getNameNode(), var.getType(), var.getJSDocInfo(), false, var.isLocal());
  }
  
  /**
   * Creates a new {@code JSElementInfo} from a property.
   * @param run  The compiler run (to be used to retrieve further information).
   * @param type  The object type the property belongs to.
   * @param propertyName  The name of the property.  It <b>must</b> exist.
   * @return  A new {@code JSElementInfo}.  
   */
  public static JSElementInfo makeFromProperty(CompilerRun run, ObjectType type, String propertyName) {
    return new JSElementInfo(
        run, type.getPropertyNode(propertyName), type.getPropertyType(propertyName), 
        getJSDocInfoOfProperty(type, propertyName), true, false);
  }

  /**
   * Creates a new {@code JSElementInfo} from a property.
   * @param run  The compiler run (to be used to retrieve further information).
   * @param type  The type the property may belong to.
   * @param propertyName  The name of the property.  It may exist or not.
   * @return  A new {@code JSElementInfo}, or null.
   */
  public static JSElementInfo makeFromPropertyOrNull(CompilerRun run, JSType type, String propertyName) {
    if (type instanceof ObjectType) {
      ObjectType objectType = (ObjectType) type;
      if (objectType.hasProperty(propertyName)) {
        Node propertyNode = objectType.getPropertyNode(propertyName);
        JSType propertyType = objectType.getPropertyType(propertyName);
        if (propertyNode != null && propertyType != null) {
          return new JSElementInfo(
              run, propertyNode, propertyType, 
              getJSDocInfoOfProperty(objectType, propertyName), true, false);
        }
      }
    }
    return null;
  }

  /**
   * Get the doc info for a property in an object type, by walking through the type hierarchy.
   * @param objectType  The objectType to which the property belong to.
   * @param propertyName  The name of the property.
   * @return  The doc info, or null if not found.
   */
  private static JSDocInfo getJSDocInfoOfProperty(ObjectType objectType, String propertyName) {
    for (; objectType != null;
        objectType = objectType.getImplicitPrototype()) {
      JSDocInfo docInfo = objectType.getOwnPropertyJSDocInfo(propertyName);
      if (docInfo != null) return docInfo;
    }
    return null;    
  }
  
  // **************************************************************************
  // Computing properties of the element
  
  private boolean isNamespace(Node node) {
    // First, check whether the node's parent or its grand parent is tagged
    // as a name space.
    Node parent = node.getParent();
    if (parent == null) return false;
    if (parent.getBooleanProp(Node.IS_NAMESPACE)) return true;
    Node parent2 = parent.getParent();
    if (parent2 == null) return false;
    if (parent2.getBooleanProp(Node.IS_NAMESPACE)) return true;
    // Special case for name spaces which are defined by var foo = {} or var foo = foo || {}.
    if (parent2.getType() != Token.SCRIPT || 
        parent.getType() != Token.VAR ||
        node.getType() != Token.NAME) return false;
    boolean hasValidNode = false;
    for (Node child: node.children()) {
      int type = child.getType();
      if (type == Token.OBJECTLIT && !child.hasChildren()) {
        hasValidNode = true;
      } else if (type == Token.OR) {
        for (Node child2: child.children()) {
          if (child2.getType() == Token.OBJECTLIT && !child2.hasChildren()) {
            hasValidNode = true;
          }
        }
      } else {
        return false;
      }
    }
    return hasValidNode;
  }
  
  /**
   * Compute the kind of the completion proposal.
   * @return  The kind of the completion proposal.
   */
  private JSElementKind computeKind() {
    if (isNamespace) return JSElementKind.NAMESPACE;
    if (docInfo != null) {
      if (docInfo.isConstructor()) {
        return JSElementKind.CLASS;
      } else if (docInfo.isInterface()) {
        return JSElementKind.INTERFACE;
      } else if (docInfo.isConstant()) {
        return JSElementKind.CONSTANT;
      }
    }
    if (type.isEnumType()) return JSElementKind.ENUM;
    if (isProperty) {
      if (type.isFunctionType()) return JSElementKind.METHOD;
      else return JSElementKind.FIELD;
    } else if (isLocalVariable) {
      return JSElementKind.LOCAL_VARIABLE;
    } else {
      return JSElementKind.GLOBAL_VARIABLE;
    }    
  }
  
  public JSElementKind getKind() { return kind; }
  public Node getNode() { return node; }
  public JSType getType() { return type; }
  
  public String getImageName() {
    switch (kind) {
    case NAMESPACE: return OwJsClosureImages.PACKAGE;
    case CLASS: 
      switch (visibility) {
      case PRIVATE: return OwJsClosureImages.CLASS_PRIVATE;
      case PROTECTED: return OwJsClosureImages.CLASS_PROTECTED;
      case PUBLIC: return OwJsClosureImages.CLASS_PUBLIC;
      }
    case INTERFACE:
      switch (visibility) {
      case PRIVATE: return OwJsClosureImages.INTERFACE_PRIVATE;
      case PROTECTED: return OwJsClosureImages.INTERFACE_PROTECTED;
      case PUBLIC: return OwJsClosureImages.INTERFACE_PUBLIC;
      }
    case ENUM: 
      switch (visibility) {
      case PRIVATE: return OwJsClosureImages.ENUM_PRIVATE;
      case PROTECTED: return OwJsClosureImages.ENUM_PROTECTED;
      case PUBLIC: return OwJsClosureImages.ENUM_PUBLIC;
      }
    case METHOD:
      switch (visibility) {
      case PRIVATE: return OwJsClosureImages.METHOD_PRIVATE;
      case PROTECTED: return OwJsClosureImages.METHOD_PROTECTED;
      case PUBLIC: return OwJsClosureImages.METHOD_PUBLIC;
      }
      break;
    case FIELD:
      switch (visibility) {
      case PRIVATE: return OwJsClosureImages.FIELD_PRIVATE;
      case PROTECTED: return OwJsClosureImages.FIELD_PROTECTED;
      case PUBLIC: return OwJsClosureImages.FIELD_PUBLIC;
      }
      break;
    case GLOBAL_VARIABLE: return OwJsClosureImages.GLOBAL_VARIABLE;
    case LOCAL_VARIABLE: return OwJsClosureImages.LOCAL_VARIABLE;
    case CONSTANT: return OwJsClosureImages.CONSTANT;
    }
    return null;
  }

  /**
   * Add a new visibility to the completion proposal.  By default, completion proposal are
   * considered as public.  The finally computed visibility is the lowest one.
   * @param extraVisibility  The visibility to add.
   */
  public void addVisibility(Visibility extraVisibility) {
    if (visibility == Visibility.PROTECTED && extraVisibility == Visibility.PRIVATE ||
        visibility == Visibility.PUBLIC && (extraVisibility == Visibility.PRIVATE || extraVisibility == Visibility.PROTECTED)) {
      visibility = extraVisibility;
    }
  }

  // **************************************************************************
  // Generating HTML string

  @Override
  public String getHTMLStringForHover() {
    if (htmlString == null) buildHTMLString();
    return htmlString;
  }

  /**
   * Build the HTML string for the proposal info.
   */
  private void buildHTMLString() {
    if (isNamespace) {
      Node namespaceNode = run.getNamespaceProvider(node.getQualifiedName());
      if (namespaceNode != null) docInfo = namespaceNode.getJSDocInfo();
    }
    buf = new StringBuffer();
    messages = OwJsClosurePlugin.getDefault().getMessages();
    if (docInfo != null) {
      HTMLPrinter.insertPageProlog(buf, 0, "");
      String qualifiedName = node.getQualifiedName();
      if (qualifiedName != null) {
        buf.append("<b>");
        buf.append(qualifiedName);
        buf.append("</b><p><hr><p>");
      }
      if (docInfo.hasFileOverview()) {
        writeFileOverview();
      } else {
        writeBlockDescription();
        writeTypeInfo();
        writeOtherInfo();
      }
      HTMLPrinter.addPageEpilog(buf);
    } else {
      Node functionNode = getFunctionNodeOfFunctionParameterNode(node);
      if (functionNode != null) {
        writeFunctionParameterInfo(functionNode, node.getString());
      }
    }
    htmlString = buf.toString();
    buf = null;
  }

  // --------------------------------------------------------------------------
  // Formatting 
  
  private void openSection(String title) {
    buf.append("<dl><dt>");
    buf.append(title);
    buf.append("</dt>");
  }
  
  private void closeSection() {
    buf.append("</dl>");
  }
  
  private void openItem() {
    buf.append("<dd>");
  }

  private void closeItem() {
    buf.append("</dd>");
  }
  
  private void openSectionAndItem(String title) {
    openSection(title);
    openItem();
  }
  
  private void closeSectionAndItem() {
    closeItem();
    closeSection();
  }

  // --------------------------------------------------------------------------
  // Description
  
  private void writeDescription(String description) {
    if (description != null) {
      buf.append(description);
      buf.append("<p>");
    }
  }
    
  private void writeFileOverview() {
    writeDescription(docInfo.getFileOverview());
  }
  
  private void writeBlockDescription() {
    writeDescription(docInfo.getBlockDescription());
  }
  
  private void writeOtherInfo() {
    // Deprecated
    if (docInfo.isDeprecated()) {
      openSection(messages.getString("jsdoc_deprecated"));
      if (docInfo.getDeprecationReason() != null) {
        openItem();
        buf.append(docInfo.getDeprecationReason());
        closeItem();
      }
      closeSection();
    }
  }

  // --------------------------------------------------------------------------
  // Type information
  // (see com.google.javascript.jscomp.TypedCodeGenerator)
  
  private void writeTypeDescription(String description) {
    if (description != null) {
      buf.append(": ");
      buf.append(description);
    }
  }

  private void writeType(JSType type) {
    buf.append("<em>");
    buf.append(type.toString());
    buf.append("</em>");
  }
  
  private void writeParameter(Node paramNode, Node typeNode, String description) {
    buf.append("<b>");
    buf.append(paramNode.getString());
    buf.append("</b>");
    if (type != null) {
      buf.append(" [<em>");
      buf.append(getParameterNodeJSDocType(typeNode).toString());
      buf.append("</em>]");
    }
    writeTypeDescription(description);
  }
  
  private void writeTypeInfo() {
    if (type == null) {
    } else if (type.isFunctionType()) {
      writeFunctionInfo(Utils.getFunctionNode(node));
    } else if (!type.isUnknownType()
        && !type.isEmptyType() && !type.isVoidType() &&
        !type.isFunctionPrototypeType() && !type.isEnumType()) {
      openSectionAndItem(messages.getString("jsdoc_type"));
      writeType(type);
      closeSectionAndItem();
    } else {
    }
  }

  /**
   * @param fnNode A node for a function for which to generate a type annotation
   */
  private void writeFunctionInfo(Node fnNode) {
    if (fnNode == null) return;
    Preconditions.checkState(fnNode.getType() == Token.FUNCTION);

    JSDocInfo fnDocInfo = NodeUtil.getFunctionJSDocInfo(fnNode);
    JSType type = fnNode.getJSType();

    if (type == null || type.isUnknownType()) {
      return;
    }

    FunctionType funType = type.toMaybeFunctionType();

    // We need to use the child nodes of the function as the nodes for the
    // parameters of the function type do not have the real parameter names.
    // FUNCTION
    //   NAME
    //   LP
    //     NAME param1
    //     NAME param2
    if (fnNode != null) {
      openSection(messages.getString("jsdoc_parameters"));
      Node paramNode = NodeUtil.getFunctionParameters(fnNode).getFirstChild();

      // Param types
      for (Node n : funType.getParameters()) {
        // Bail out if the paramNode is not there.
        if (paramNode == null) {
          break;
        }
        openItem();
        writeParameter(paramNode, n, fnDocInfo.getDescriptionForParameter(paramNode.getString()));
        closeItem();
        paramNode = paramNode.getNext();
      }
      closeSection();
    }

    // Return type
    JSType retType = funType.getReturnType();
    if (retType != null && !retType.isUnknownType() && !retType.isEmptyType()) {
      openSectionAndItem(messages.getString("jsdoc_return"));
      writeType(retType);
      writeTypeDescription(fnDocInfo.getReturnDescription());
      closeSectionAndItem();
    }

    // Constructor/interface
    if (funType.isConstructor() || funType.isInterface()) {

      FunctionType superConstructor = funType.getSuperClassConstructor();

      if (superConstructor != null) {
        ObjectType superInstance =
          funType.getSuperClassConstructor().getInstanceType();
        if (!superInstance.toString().equals("Object")) {
          openSectionAndItem(messages.getString("jsdoc_extends"));
          buf.append(superInstance.toString());
          closeSectionAndItem();
        }
      }

      if (funType.isInterface()) {
        for (ObjectType interfaceType : funType.getExtendedInterfaces()) {
          openSectionAndItem(messages.getString("jsdoc_extends"));
          buf.append(interfaceType.toString());
          closeSectionAndItem();
        }
      }

      // Avoid duplicates, add implemented type to a set first
      Set<String> interfaces = Sets.newTreeSet();
      for (ObjectType interfaze : funType.getImplementedInterfaces()) {
        interfaces.add(interfaze.toString());
      }
      if (!interfaces.isEmpty()) {
        openSectionAndItem(messages.getString("jsdoc_implements"));
        boolean first = true;
        for (String interfaze : interfaces) {
          if (first) first = false; else buf.append("<p>");
          buf.append(interfaze.toString());
        }
        closeSectionAndItem();
      }
    }
  }

  /**
   * Creates a JSDoc-suitable String representation the type of a parameter.
   *
   * @param parameterNode The parameter node.
   */
  private String getParameterNodeJSDocType(Node parameterNode) {
    JSType parameterType = parameterNode.getJSType();
    String typeString;

    // Emit unknown types as '*' (AllType) since '?' (UnknownType) is not
    // a valid JSDoc type.
    if (parameterType.isUnknownType()) {
      typeString = "*";
    } else {
      // Fix-up optional and vararg parameters to match JSDoc type language
      if (parameterNode.isOptionalArg()) {
        typeString = parameterType.restrictByNotNullOrUndefined() + "=";
      } else if (parameterNode.isVarArgs()) {
        typeString = "..." + parameterType.restrictByNotNullOrUndefined();
      } else {
        typeString = parameterType.restrictByNotNullOrUndefined().toString();
      }
    }

    return typeString;
  }

  
  // **************************************************************************
  // Function parameters

  private void writeFunctionParameterInfo(Node functionNode, String parameterName) {
    JSDocInfo docInfo = functionNode.getJSDocInfo();
    if (docInfo != null) {
      String description = docInfo.getDescriptionForParameter(parameterName);
      if (description != null) {
        buf.append(description);
        buf.append("<p>");        
      }
      openSectionAndItem(messages.getString("jsdoc_type"));
      buf.append("<em>");
      buf.append(getParameterNodeJSDocType(node));
      buf.append("</em>");
      closeSectionAndItem();
    }
  }


  // **************************************************************************
  // Helper function for parameter nodes
  
  private Node getFunctionNodeOfFunctionParameterNode(Node node) {
    if (node.getType() == Token.NAME) {
      Node parent = node.getParent();
      if (parent != null && parent.getType() == Token.LP) {
        Node parent2 = parent.getParent();
        if (parent2 != null && parent2.getType() == Token.FUNCTION) 
          return parent2;
      }
    }
    return null;
  }
  
}
