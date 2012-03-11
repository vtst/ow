package net.vtst.ow.eclipse.js.closure.contentassist;

import java.util.Set;

import net.vtst.ow.eclipse.js.closure.OwJsClosureMessages;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.util.HTMLPrinter;
import net.vtst.ow.eclipse.js.closure.util.Utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.javascript.jscomp.NodeUtil;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;
import com.google.javascript.rhino.jstype.FunctionType;
import com.google.javascript.rhino.jstype.JSType;
import com.google.javascript.rhino.jstype.ObjectType;

public class ClosureAdditionalProposalInfo implements IAdditionalProposalInfo {
  
  private OwJsClosureMessages messages;
  
  private Node node;
  private JSDocInfo docInfo;
  private JSType type;
  private StringBuffer buf;
  private String htmlString;

  /**
   * Create a new additional proposal info.
   * @param node  The node to which the proposal info is relative.
   * @param docInfo  The doc info to use for filling the proposal info.
   * @param type  The type to use for filling the proposal info.
   */
  public ClosureAdditionalProposalInfo(Node node, JSDocInfo docInfo, JSType type) {
    this.node = node;
    this.docInfo = docInfo;
    this.type = type;
  }

  @Override
  public String getHTMLString() {
    if (htmlString == null) buildHTMLString();
    return htmlString;
  }

  /**
   * Build the HTML string for the proposal info.
   */
  private void buildHTMLString() {
    buf = new StringBuffer();
    messages = OwJsClosurePlugin.getDefault().getMessages();
    if (docInfo != null) {
      HTMLPrinter.insertPageProlog(buf, 0, "");
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

  // **************************************************************************
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

  // **************************************************************************
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


  // **************************************************************************
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
        !type.isFunctionPrototypeType()) {
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
