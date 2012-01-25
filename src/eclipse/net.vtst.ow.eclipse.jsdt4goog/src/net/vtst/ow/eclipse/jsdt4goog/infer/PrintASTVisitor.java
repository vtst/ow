package net.vtst.ow.eclipse.jsdt4goog.infer;

import org.eclipse.wst.jsdt.core.compiler.IProblem;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.infer.InferredAttribute;
import org.eclipse.wst.jsdt.core.infer.InferredMethod;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.core.ast.ASTVisitor;
import org.eclipse.wst.jsdt.core.ast.IAND_AND_Expression;
import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IAllocationExpression;
import org.eclipse.wst.jsdt.core.ast.IArgument;
import org.eclipse.wst.jsdt.core.ast.IArrayAllocationExpression;
import org.eclipse.wst.jsdt.core.ast.IArrayInitializer;
import org.eclipse.wst.jsdt.core.ast.IArrayQualifiedTypeReference;
import org.eclipse.wst.jsdt.core.ast.IArrayReference;
import org.eclipse.wst.jsdt.core.ast.IArrayTypeReference;
import org.eclipse.wst.jsdt.core.ast.IAssignment;
import org.eclipse.wst.jsdt.core.ast.IBinaryExpression;
import org.eclipse.wst.jsdt.core.ast.IBlock;
import org.eclipse.wst.jsdt.core.ast.IBreakStatement;
import org.eclipse.wst.jsdt.core.ast.ICaseStatement;
import org.eclipse.wst.jsdt.core.ast.ICompoundAssignment;
import org.eclipse.wst.jsdt.core.ast.IConditionalExpression;
import org.eclipse.wst.jsdt.core.ast.IConstructorDeclaration;
import org.eclipse.wst.jsdt.core.ast.IContinueStatement;
import org.eclipse.wst.jsdt.core.ast.IDoStatement;
import org.eclipse.wst.jsdt.core.ast.IDoubleLiteral;
import org.eclipse.wst.jsdt.core.ast.IEmptyStatement;
import org.eclipse.wst.jsdt.core.ast.IEqualExpression;
import org.eclipse.wst.jsdt.core.ast.IExplicitConstructorCall;
import org.eclipse.wst.jsdt.core.ast.IExtendedStringLiteral;
import org.eclipse.wst.jsdt.core.ast.IFalseLiteral;
import org.eclipse.wst.jsdt.core.ast.IFieldDeclaration;
import org.eclipse.wst.jsdt.core.ast.IFieldReference;
import org.eclipse.wst.jsdt.core.ast.IForInStatement;
import org.eclipse.wst.jsdt.core.ast.IForStatement;
import org.eclipse.wst.jsdt.core.ast.IForeachStatement;
import org.eclipse.wst.jsdt.core.ast.IFunctionCall;
import org.eclipse.wst.jsdt.core.ast.IFunctionDeclaration;
import org.eclipse.wst.jsdt.core.ast.IFunctionExpression;
import org.eclipse.wst.jsdt.core.ast.IIfStatement;
import org.eclipse.wst.jsdt.core.ast.IImportReference;
import org.eclipse.wst.jsdt.core.ast.IInitializer;
import org.eclipse.wst.jsdt.core.ast.IInstanceOfExpression;
import org.eclipse.wst.jsdt.core.ast.IIntLiteral;
import org.eclipse.wst.jsdt.core.ast.IJsDoc;
import org.eclipse.wst.jsdt.core.ast.IJsDocAllocationExpression;
import org.eclipse.wst.jsdt.core.ast.IJsDocArgumentExpression;
import org.eclipse.wst.jsdt.core.ast.IJsDocArrayQualifiedTypeReference;
import org.eclipse.wst.jsdt.core.ast.IJsDocArraySingleTypeReference;
import org.eclipse.wst.jsdt.core.ast.IJsDocFieldReference;
import org.eclipse.wst.jsdt.core.ast.IJsDocImplicitTypeReference;
import org.eclipse.wst.jsdt.core.ast.IJsDocMessageSend;
import org.eclipse.wst.jsdt.core.ast.IJsDocQualifiedTypeReference;
import org.eclipse.wst.jsdt.core.ast.IJsDocReturnStatement;
import org.eclipse.wst.jsdt.core.ast.IJsDocSingleNameReference;
import org.eclipse.wst.jsdt.core.ast.IJsDocSingleTypeReference;
import org.eclipse.wst.jsdt.core.ast.ILabeledStatement;
import org.eclipse.wst.jsdt.core.ast.IListExpression;
import org.eclipse.wst.jsdt.core.ast.ILocalDeclaration;
import org.eclipse.wst.jsdt.core.ast.INullLiteral;
import org.eclipse.wst.jsdt.core.ast.IOR_OR_Expression;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteral;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteralField;
import org.eclipse.wst.jsdt.core.ast.IPostfixExpression;
import org.eclipse.wst.jsdt.core.ast.IPrefixExpression;
import org.eclipse.wst.jsdt.core.ast.IQualifiedAllocationExpression;
import org.eclipse.wst.jsdt.core.ast.IQualifiedNameReference;
import org.eclipse.wst.jsdt.core.ast.IQualifiedThisReference;
import org.eclipse.wst.jsdt.core.ast.IQualifiedTypeReference;
import org.eclipse.wst.jsdt.core.ast.IRegExLiteral;
import org.eclipse.wst.jsdt.core.ast.IReturnStatement;
import org.eclipse.wst.jsdt.core.ast.IScriptFileDeclaration;
import org.eclipse.wst.jsdt.core.ast.ISingleNameReference;
import org.eclipse.wst.jsdt.core.ast.ISingleTypeReference;
import org.eclipse.wst.jsdt.core.ast.IStringLiteral;
import org.eclipse.wst.jsdt.core.ast.IStringLiteralConcatenation;
import org.eclipse.wst.jsdt.core.ast.ISuperReference;
import org.eclipse.wst.jsdt.core.ast.ISwitchStatement;
import org.eclipse.wst.jsdt.core.ast.IThisReference;
import org.eclipse.wst.jsdt.core.ast.IThrowStatement;
import org.eclipse.wst.jsdt.core.ast.ITrueLiteral;
import org.eclipse.wst.jsdt.core.ast.ITryStatement;
import org.eclipse.wst.jsdt.core.ast.ITypeDeclaration;
import org.eclipse.wst.jsdt.core.ast.IUnaryExpression;
import org.eclipse.wst.jsdt.core.ast.IUndefinedLiteral;
import org.eclipse.wst.jsdt.core.ast.IWhileStatement;
import org.eclipse.wst.jsdt.core.ast.IWithStatement;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;

/**
 * AST visitor which pretty-prints the structure of the AST.  This is useful for debugging.
 * @author Vincent Simonet
 */
public class PrintASTVisitor extends ASTVisitor {

  private int level = 0;
  
  public boolean doVisit(Object node) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < level; ++i) buf.append("  ");
    buf.append(node.getClass().getName());
    buf.append(": ");
    buf.append(node.toString());
    System.out.println(buf.toString());
    LocalDeclaration decl;
    
    ++level;
    return true;
  }

  public void doEndVisit(Object node) {
    --level;
  }


  
  public void acceptProblem(IProblem problem) {
  }
  
  public void endVisit(IAllocationExpression node) {
    doEndVisit(node);
  }
  public void endVisit(IAND_AND_Expression node) {
    doEndVisit(node);
  }
  public void endVisit(IArgument node) {
    doEndVisit(node);
  }

  public void endVisit(IArrayAllocationExpression node) {
    doEndVisit(node);
  }
  public void endVisit(IArrayInitializer node) {
    doEndVisit(node);
  }
  public void endVisit(IArrayQualifiedTypeReference node) {
    doEndVisit(node);
  }
  public void endVisit(IArrayReference node) {
    doEndVisit(node);
  }
  public void endVisit(IArrayTypeReference node) {
    doEndVisit(node);
  }
  public void endVisit(IAssignment node) {
    doEndVisit(node);
  }
  public void endVisit(IBinaryExpression node) {
    doEndVisit(node);
  }
  public void endVisit(IBlock node) {
    doEndVisit(node);
  }
  public void endVisit(IBreakStatement node) {
    doEndVisit(node);
  }
  public void endVisit(ICaseStatement node) {
    doEndVisit(node);
  }
  public void endVisit(IScriptFileDeclaration node) {    
    doEndVisit(node);
  }
  public void endVisit(ICompoundAssignment node) {
    doEndVisit(node);
  }
  public void endVisit(IConditionalExpression node) {
    doEndVisit(node);
  }
  public void endVisit(IConstructorDeclaration node) {
    doEndVisit(node);
  }
  public void endVisit(IContinueStatement node) {
    doEndVisit(node);
  }
  public void endVisit(IDoStatement node) {
    doEndVisit(node);
  }
  public void endVisit(IDoubleLiteral node) {
    doEndVisit(node);
  }
  public void endVisit(IEmptyStatement node) {
    doEndVisit(node);
  }
  public void endVisit(IEqualExpression node) {
    doEndVisit(node);
  }
  public void endVisit(IExplicitConstructorCall node) {
    doEndVisit(node);
  }
  public void endVisit(IExtendedStringLiteral node) {
    doEndVisit(node);
  }
  public void endVisit(IFalseLiteral node) {
    doEndVisit(node);
  }
  public void endVisit(IFieldDeclaration node) {
    doEndVisit(node);
  }
  
  public void endVisit(IFieldReference node) {
    doEndVisit(node);
  }
  
  public void endVisit(IForeachStatement node) {
    doEndVisit(node);
  }
  public void endVisit(IForStatement node) {
    doEndVisit(node);
  }
  public void endVisit(IForInStatement node) {
    doEndVisit(node);
  }

  public void endVisit(IFunctionExpression node) {
  }

  public void endVisit(IIfStatement node) {
    doEndVisit(node);
  }
  public void endVisit(IImportReference node) {
    doEndVisit(node);
  }
  public void endVisit(InferredType node) {
    doEndVisit(node);
  }

  public void endVisit(IInitializer node) {
    doEndVisit(node);
  }
  public void endVisit(IInstanceOfExpression node) {
    doEndVisit(node);
  }
  public void endVisit(IIntLiteral node) {
    doEndVisit(node);
  }
  public void endVisit(IJsDoc node) {
    doEndVisit(node);
  }
  public void endVisit(IJsDocAllocationExpression node) {
    doEndVisit(node);
  }
  public void endVisit(IJsDocArgumentExpression node) {
    doEndVisit(node);
  }
  public void endVisit(IJsDocArrayQualifiedTypeReference node) {
    doEndVisit(node);
  }
  public void endVisit(IJsDocArraySingleTypeReference node) {
    doEndVisit(node);
  }
  public void endVisit(IJsDocFieldReference node) {
    doEndVisit(node);
  }
  public void endVisit(IJsDocImplicitTypeReference node) {
    doEndVisit(node);
  }
  public void endVisit(IJsDocMessageSend node) {
    doEndVisit(node);
  }
  public void endVisit(IJsDocQualifiedTypeReference node) {
    doEndVisit(node);
  }
  public void endVisit(IJsDocReturnStatement node) {
    doEndVisit(node);
  }
  public void endVisit(IJsDocSingleNameReference node) {
    doEndVisit(node);
  }
  public void endVisit(IJsDocSingleTypeReference node) {
    doEndVisit(node);
  }
  public void endVisit(ILabeledStatement node) {
    doEndVisit(node);
  }
  public void endVisit(ILocalDeclaration node) {
    doEndVisit(node);
  }
  public void endVisit(IListExpression node) {
    doEndVisit(node);
  }
  public void endVisit(IFunctionCall node) {
    doEndVisit(node);
  }
  public void endVisit(IFunctionDeclaration node) {
    doEndVisit(node);
  }
  public void endVisit(IStringLiteralConcatenation node) {
    doEndVisit(node);
  }
  public void endVisit(INullLiteral node) {
    doEndVisit(node);
  }
  public void endVisit(IOR_OR_Expression node) {
    doEndVisit(node);
  }
  public void endVisit(IPostfixExpression node) {
    doEndVisit(node);
  }
  public void endVisit(IPrefixExpression node) {
    doEndVisit(node);
  }
  public void endVisit(IQualifiedAllocationExpression node) {
    doEndVisit(node);
  }
  public void endVisit(IQualifiedNameReference node) {
    doEndVisit(node);
  }
  public void endVisit(IQualifiedThisReference node) {
    doEndVisit(node);
  }
  public void endVisit(IQualifiedTypeReference node) {
    doEndVisit(node);
  }

  public void endVisit(IRegExLiteral node) {
    doEndVisit(node);
  }


  public void endVisit(IReturnStatement node) {
    doEndVisit(node);
  }
  public void endVisit(ISingleNameReference node) {
    doEndVisit(node);
  }
  
  public void endVisit(ISingleTypeReference node) {
    doEndVisit(node);
  }
  public void endVisit(IStringLiteral node) {
    doEndVisit(node);
  }
  public void endVisit(ISuperReference node) {
    doEndVisit(node);
  }
  public void endVisit(ISwitchStatement node) {
    doEndVisit(node);
  }

  public void endVisit(IThisReference node) {
    doEndVisit(node);
  }
  public void endVisit(IThrowStatement node) {
    doEndVisit(node);
  }
  public void endVisit(ITrueLiteral node) {
    doEndVisit(node);
  }
  public void endVisit(ITryStatement node) {
    doEndVisit(node);
  }
  public void endVisit(ITypeDeclaration node) {
    doEndVisit(node);
  }
  public void endVisit(IUnaryExpression node) {
    doEndVisit(node);
  }
  public void endVisit(IUndefinedLiteral node) {
    doEndVisit(node);
  }

  public void endVisit(IWhileStatement node) {
    doEndVisit(node);
  }
  public void endVisit(IWithStatement node) {
    doEndVisit(node);
  }
  public boolean visit(IAllocationExpression node) {
    return doVisit(node);
  }
  public boolean visit(IAND_AND_Expression node) {
    return doVisit(node);
  }
  public boolean visit(IArgument node) {
    return doVisit(node);
  }

  public boolean visit(IArrayAllocationExpression node) {
    return doVisit(node);
  }
  public boolean visit(IArrayInitializer node) {
    return doVisit(node);
  }
  public boolean visit(IArrayQualifiedTypeReference node) {
    return doVisit(node);
  }

  public boolean visit(IArrayReference node) {
    return doVisit(node);
  }
  public boolean visit(IArrayTypeReference node) {
    return doVisit(node);
  }
  public boolean visit(IAssignment node) {
    return doVisit(node);
  }
  public boolean visit(IBinaryExpression node) {
    return doVisit(node);
  }
  public boolean visit(IBlock node) {
    return doVisit(node);
  }
  public boolean visit(IBreakStatement node) {
    return doVisit(node);
  }
  public boolean visit(ICaseStatement node) {
    return doVisit(node);
  }
  public boolean visit(IScriptFileDeclaration node) {
    return doVisit(node);
  }
  public boolean visit(ICompoundAssignment node) {
    return doVisit(node);
  }
  public boolean visit(IConditionalExpression node) {
    return doVisit(node);
  }
  public boolean visit(IConstructorDeclaration node) {
    return doVisit(node);
  }
  public boolean visit(IContinueStatement node) {
    return doVisit(node);
  }
  public boolean visit(IDoStatement node) {
    return doVisit(node);
  }
  public boolean visit(IDoubleLiteral node) {
    return doVisit(node);
  }
  public boolean visit(IEmptyStatement node) {
    return doVisit(node);
  }
  public boolean visit(IEqualExpression node) {
    return doVisit(node);
  }
  public boolean visit(IExplicitConstructorCall node) {
    return doVisit(node);
  }
  public boolean visit(IExtendedStringLiteral node) {
    return doVisit(node);
  }
  public boolean visit(IFalseLiteral node) {
    return doVisit(node);
  }
  public boolean visit(IFieldDeclaration node) {
    return doVisit(node);
  }
  public boolean visit(IFieldReference node) {
    return doVisit(node);
  }
  public boolean visit(IForeachStatement node) {
    return doVisit(node);
  }
  public boolean visit(IForInStatement node) {
    return doVisit(node);
  }
  public boolean visit(IForStatement node) {
    return doVisit(node);
  }
  public boolean visit(IFunctionExpression node) {
    System.out.println(node.getMethodDeclaration().getClass().getName());
    System.out.println(node.getMethodDeclaration().compilationResult().getClass().getName());
    return doVisit(node);
  }
  public boolean visit(IIfStatement node) {
    return doVisit(node);
  }
  public boolean visit(IImportReference node) {
    return doVisit(node);
  }

  public boolean visit(InferredType node) {
    return doVisit(node);
  }

  public boolean visit(InferredMethod node) {
    return doVisit(node);
  }

  public boolean visit(InferredAttribute node) {
    return doVisit(node);
  }
  public boolean visit(IInitializer node) {
    return doVisit(node);
  }
  public boolean visit(IInstanceOfExpression node) {
    return doVisit(node);
  }
  public boolean visit(IIntLiteral node) {
    return doVisit(node);
  }
  public boolean visit(IJsDoc node) {
    return doVisit(node);
  }

  public boolean visit(IJsDocAllocationExpression node) {
    return doVisit(node);
  }

  public boolean visit(IJsDocArgumentExpression node) {
    return doVisit(node);
  }

  public boolean visit(IJsDocArrayQualifiedTypeReference node) {
    return doVisit(node);
  }

  public boolean visit(IJsDocArraySingleTypeReference node) {
    return doVisit(node);
  }

  public boolean visit(IJsDocFieldReference node) {
    return doVisit(node);
  }

  public boolean visit(IJsDocImplicitTypeReference node) {
    return doVisit(node);
  }

  public boolean visit(IJsDocMessageSend node) {
    return doVisit(node);
  }

  public boolean visit(IJsDocQualifiedTypeReference node) {
    return doVisit(node);
  }

  public boolean visit(IJsDocReturnStatement node) {
    return doVisit(node);
  }

  public boolean visit(IJsDocSingleNameReference node) {
    return doVisit(node);
  }

  public boolean visit(IJsDocSingleTypeReference node) {
    return doVisit(node);
  }

  public boolean visit(ILabeledStatement node) {
    return doVisit(node);
  }
  public boolean visit(ILocalDeclaration node) {
    return doVisit(node);
  }
  public boolean visit(IListExpression node) {
    return doVisit(node);
  }
  public boolean visit(IFunctionCall node) {
    return doVisit(node);
  }
  public boolean visit(IFunctionDeclaration node) {
    return doVisit(node);
  }
  public boolean visit(IStringLiteralConcatenation node) {
    return doVisit(node);
  }
  public boolean visit(INullLiteral node) {
    return doVisit(node);
  }
  public boolean visit(IOR_OR_Expression node) {
    return doVisit(node);
  }
  public boolean visit(IPostfixExpression node) {
    return doVisit(node);
  }
  public boolean visit(IPrefixExpression node) {
    return doVisit(node);
  }
  public boolean visit(IQualifiedAllocationExpression node) {
    return doVisit(node);
  }
  public boolean visit(IQualifiedNameReference node) {
    return doVisit(node);
  }

  public boolean visit(IQualifiedThisReference node) {
    return doVisit(node);
  }

  public boolean visit(IQualifiedTypeReference node) {
    return doVisit(node);
  }

  public boolean visit(IRegExLiteral node) {
    return doVisit(node);
  }
  public boolean visit(IReturnStatement node) {
    return doVisit(node);
  }
  public boolean visit(ISingleNameReference node) {
    return doVisit(node);
  }

  public boolean visit(ISingleTypeReference node) {
    return doVisit(node);
  }

  public boolean visit(IStringLiteral node) {
    return doVisit(node);
  }
  public boolean visit(ISuperReference node) {
    return doVisit(node);
  }
  public boolean visit(ISwitchStatement node) {
    return doVisit(node);
  }

  public boolean visit(IThisReference node) {
    return doVisit(node);
  }

  public boolean visit(IThrowStatement node) {
    return doVisit(node);
  }
  public boolean visit(ITrueLiteral node) {
    return doVisit(node);
  }
  public boolean visit(ITryStatement node) {
    return doVisit(node);
  }
  public boolean visit(ITypeDeclaration node) {
    return doVisit(node);
  }

  public boolean visit(IUnaryExpression node) {
    return doVisit(node);
  }
  public boolean visit(IUndefinedLiteral node) {
    return doVisit(node);
  }
  public boolean visit(IWhileStatement node) {
    return doVisit(node);
  }
  public boolean visit(IWithStatement node) {
    return doVisit(node);
  }

  public boolean visit(IObjectLiteral node) {
    return doVisit(node);
  }
  public void endVisit(IObjectLiteral node) {
  }
  public boolean visit(IObjectLiteralField node) {
    return doVisit(node);
  }
  public void endVisit(IObjectLiteralField node) {
  }

}
