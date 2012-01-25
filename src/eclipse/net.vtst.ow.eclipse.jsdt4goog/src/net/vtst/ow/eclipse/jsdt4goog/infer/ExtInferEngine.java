// net.vtst.ow.eclipse.jsdt4goog
// (c) Vincent Simonet, 2011.

package net.vtst.ow.eclipse.jsdt4goog.infer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.wst.jsdt.core.ast.IASTNode;
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IFunctionCall;
import org.eclipse.wst.jsdt.core.infer.InferEngine;
import org.eclipse.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.Assignment;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.Expression;
import org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.FunctionExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ObjectLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.ProgramElement;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteral;

/**
 * This class extends the base inference engine from JSDT in order to handle some special
 * constructs of the Google Closure Library.  It works by modifying the AST of the processed
 * compilation unit.
 * @author Vincent Simonet
 */
public class ExtInferEngine extends InferEngine {

  private CompilationUnitDeclaration compUnit;
  private static final int GOOG_NONE = 0;
  private static final int GOOG_REQUIRE = 1;
  private static final int GOOG_PROVIDE = 2;
  private static final int GOOG_INHERITS = 3;
  
  private static final char[] GOOG_NAME = new char[]{'g','o','o','g'};
  private static final char[] GOOG_REQUIRE_NAME = new char[]{'r','e','q','u','i','r','e'};
  private static final char[] GOOG_PROVIDE_NAME = new char[]{'p','r','o','v','i','d','e'};
  private static final char[] GOOG_INHERITS_NAME = new char[]{'i','n','h','e','r','i','t','s'};

  private static final char[] NAMESPACE = new char[]{'n','a','m','e','s','p','a','c','e'};
  private static final char[] PROTOTYPE = new char[]{'p','r','o','t','o','t','y','p','e'};
  
  private void copySourcePositions(IASTNode from, ASTNode to) {
    to.sourceStart = from.sourceStart();
    to.sourceEnd = from.sourceEnd();
  }
  
  /**
   * Add the initialization code which is needed at the top of a JavaScript file for
   * the AST generated for Google Closure directives.
   * NAMESPACE = function(){};
   * @param statements
   */
  private void addInitialization(List<ProgramElement> statements) {
    Expression lhs = new SingleNameReference(NAMESPACE, 0, 0);
    MethodDeclaration methodDeclaration = new MethodDeclaration(this.compUnit.compilationResult);
    FunctionExpression rhs = new FunctionExpression(methodDeclaration);
    Assignment assignment = new Assignment(lhs, rhs, 0);
    statements.add(assignment);
  }
  
  /*
  org.eclipse.wst.jsdt.internal.compiler.ast.Assignment: NAMESPACE = function () {}
    org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference: NAMESPACE
    org.eclipse.wst.jsdt.internal.compiler.ast.FunctionExpression: function () {}
      org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration: function () {}

   */
  
  /**
   * Test whether a function call is a directive of the Google Closure Libary (like
   * goog.provide, goog.require and goog.inherits).
   * @param functionCall
   * @return  The ID of the directive, see the above constants.
   */
  private int getGoogDirective(IFunctionCall functionCall) {
    IExpression receiver = functionCall.getReceiver();
    if (receiver instanceof SingleNameReference) {
      char[] name = ((SingleNameReference) receiver).getToken();
      if (Arrays.equals(GOOG_NAME, name)) {
        char[] selector = functionCall.getSelector();
        if (Arrays.equals(GOOG_REQUIRE_NAME, selector)) return GOOG_REQUIRE;
        if (Arrays.equals(GOOG_PROVIDE_NAME, selector)) return GOOG_PROVIDE;
        if (Arrays.equals(GOOG_INHERITS_NAME, selector)) return GOOG_INHERITS;
      }
    }
    return GOOG_NONE;
  }

  /**
   * Get the value of the first argument of a function call if it is a string literal.
   * Return null in all other cases.
   * @param functionCall
   * @return The literal value, or null.
   */
  private String getStringArgument(IFunctionCall functionCall) {
    if (functionCall.getArguments().length < 1) return null;
    IExpression arg0 = functionCall.getArguments()[0];
    if (!(arg0 instanceof StringLiteral)) return null;
    String s = arg0.toString();
    if (s.length() < 2) return null;
    return s.substring(1, s.length() - 1);
  }
  
  
  /**
   * Adapt a goog.provide directive into legacy code.
   * <code>
   *   goog.provide('x.y.z')
   *   ->
   *   x = {}
   *   x.prototype = new NAMESPACE;
   *   x.y = {}
   *   x.y.prototype = new NAMESPACE;
   *   x.y.z = {}
   *   x.y.z.prototype = new NAMESPACE;
   * </code>
   * @param functionCall  The function call containing the goog.provide directive.
   * @return
   */
  protected void adaptGoogProvide(IFunctionCall functionCall, List<ProgramElement> statements) {
    String packageName = getStringArgument(functionCall);
    if (packageName == null) return;
    int start = functionCall.getArguments()[0].sourceStart();
    String[] packagePath = packageName.split("\\.");
    for (int i = 0; i < packagePath.length; ++i) {
      int end = start + packagePath[i].length();
      // x.y.z = {}
      /*
      Expression lhs1 = createLeftHandSide(packagePath, i, start, end);
      copySourcePositions(functionCall, lhs1);
      ObjectLiteral rhs1 = new ObjectLiteral();
      rhs1.sourceStart = start;
      rhs1.sourceEnd = end;
      Assignment assignment1 = new Assignment(lhs1, rhs1, functionCall.sourceEnd());
      copySourcePositions(functionCall, assignment1);
      statements.add(assignment1);
      */
      // x.y.z.prototype = new NAMESPACE
      FieldReference lhs2 = new FieldReference(PROTOTYPE, functionCall.sourceStart());
      lhs2.receiver = createLeftHandSide(packagePath, i, start, end);
      copySourcePositions(functionCall, lhs2);
      AllocationExpression rhs2 = new AllocationExpression();
      rhs2.member = new SingleNameReference(NAMESPACE, functionCall.sourceStart(), functionCall.sourceEnd());
      rhs2.arguments = new Expression[0];
      copySourcePositions(functionCall, rhs2);
      Assignment assignment2 = new Assignment(lhs2, rhs2, functionCall.sourceEnd());
      copySourcePositions(functionCall, assignment2);
      statements.add(assignment2);
      start += 1 + packagePath[i].length();
    }
  }
  
  private Expression createLeftHandSide(String[] packagePath, int index, int sourceStart, int sourceEnd) {
    Expression lhs = new SingleNameReference(packagePath[0].toCharArray(), sourceStart, sourceEnd);
    for (int j = 1; j <= index; ++j) {
      FieldReference fieldRef = new FieldReference(packagePath[j].toCharArray(), sourceStart);
      fieldRef.receiver = lhs;
      lhs = fieldRef;
    }   
    return lhs;
  }
  /*
   *   org.eclipse.wst.jsdt.internal.compiler.ast.Assignment: yop.bar.foo.prototype = new NAMESPACE
    org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference: yop.bar.foo.prototype
      org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference: yop.bar.foo
        org.eclipse.wst.jsdt.internal.compiler.ast.FieldReference: yop.bar
          org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference: yop
    org.eclipse.wst.jsdt.internal.compiler.ast.AllocationExpression: new NAMESPACE
      org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference: NAMESPACE

   */
  
  /**
   * Adapt a goog.inherits directive into legacy code.
   * <code>
   *   goog.inherits(class1, class2)
   *   ->
   *   class1.prototype = new class2;
   * </code>
   * @param functionCall  The function call containing the goog.provide directive.
   * @return
   */
  protected void adaptGoogInherits(IFunctionCall functionCall, List<ProgramElement> statements) {
    IExpression[] arguments = functionCall.getArguments();
    if (arguments.length < 2 ||
        !(arguments[0] instanceof Expression) || 
        !(arguments[1] instanceof Expression)) return;
    FieldReference lhs = new FieldReference(PROTOTYPE, arguments[0].sourceStart());
    lhs.receiver = (Expression) arguments[0];
    copySourcePositions(arguments[0], lhs);
    AllocationExpression rhs = new AllocationExpression();
    rhs.member = (Expression) arguments[1];
    rhs.arguments = new Expression[0];
    copySourcePositions(arguments[1], rhs);
    Assignment assignment = new Assignment(lhs, rhs, functionCall.sourceEnd());
    copySourcePositions(functionCall, assignment);
    statements.add(assignment);
  }
  
  /**
   * Modify in place the compilation unit, to replace the directives from the Google Closure
   * Library by legacy code which is correctly interpreted by the compiler.
   * @param compUnit  The compilation unit to modify.
   */
  protected void adaptCompilationUnit(CompilationUnitDeclaration compUnit) {
    List<ProgramElement> newStatements = new ArrayList<ProgramElement>();
    addInitialization(newStatements);
    for (ProgramElement statement: compUnit.statements) {
      if (statement instanceof IFunctionCall) {
        IFunctionCall functionCall = (IFunctionCall) statement;
        switch (getGoogDirective(functionCall)) {
        case GOOG_NONE:
          newStatements.add(statement);
          break;
        case GOOG_REQUIRE:
          break;
        case GOOG_PROVIDE:
          adaptGoogProvide(functionCall, newStatements);
          break;
        case GOOG_INHERITS:
          adaptGoogInherits(functionCall, newStatements);
          break;
        }
      } else {
        newStatements.add(statement);
      }
    }
    compUnit.statements = newStatements.toArray(new ProgramElement[0]);
  }
  
  @Override
  public void setCompilationUnit(CompilationUnitDeclaration compUnit) {
    this.compUnit = compUnit;
    if (new String(compUnit.getFileName()).startsWith("/test/goog")) {
      System.out.println("YOP");
    }
    //compUnit.traverse(new PrintASTVisitor());
    System.out.println(compUnit.getFileName());
    //adaptCompilationUnit(compUnit);
    super.setCompilationUnit(compUnit);
  }
  
  @Override
  public void doInfer() {
    super.doInfer();
  }
}
