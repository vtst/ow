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

    private static class ImplicitRequiresCallback extends AbstractShallowCallback {

        
        private List<String> implicitDeps;

        public ImplicitRequiresCallback(List<String> implicitDeps) {
            this.implicitDeps = implicitDeps;
        }

        @Override
        public void visit(NodeTraversal t, Node n, Node parent) {
            JSDocInfo info = n.getJSDocInfo();
            
            if (info != null) {
                for (Node node : info.getTypeNodes()) {
                    if (! node.isString()) {
                        throw new RuntimeException("we expected to always get strings.");
                    }
                    implicitDeps.add(node.getString());
                }
            }
        }
    }

    private GetImplicitDependenciesNodeTraversal(AbstractCompiler compiler, Callback callback) {
        super(compiler, callback);
    }
     
    public static boolean run(AbstractCompiler compiler, Node root, List<String> implicitDeps) {
        try {
            
            
            ImplicitRequiresCallback callback = new ImplicitRequiresCallback(implicitDeps);
            
            new NodeTraversal(compiler, callback).traverse(root);
            
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }}
