package net.vtst.ow.closure.compiler.deps;

import java.util.Iterator;
import java.util.List;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.NodeTraversal;
import com.google.javascript.jscomp.Scope.Var;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.JSTypeExpression;
import com.google.javascript.rhino.Node;


public class GetImplicitDependenciesNodeTraversal extends NodeTraversal {

    private GetImplicitDependenciesNodeTraversal(AbstractCompiler compiler, ReferenceCollectingCallback callback) {
        super(compiler, callback);
    }
     
    public static boolean run(AbstractCompiler compiler, Node root, List<String> implicitDeps) {
        try {
            ReferenceCollectingCallback callback = new ReferenceCollectingCallback(
                compiler, ReferenceCollectingCallback.DO_NOTHING_BEHAVIOR);
            new GetImplicitDependenciesNodeTraversal(compiler, callback).traverse(root);
            Iterator<Var> iterator = callback.getAllSymbols().iterator();
            while(iterator.hasNext()) {
                Var var = iterator.next();
                System.out.println(var.toString());
            }
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }}
