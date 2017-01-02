package net.vtst.ow.closure.compiler.deps.test;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import net.vtst.ow.closure.compiler.deps.GetImplicitDependenciesNodeTraversal;



import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CodingConvention;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.jscomp.DiagnosticGroups;
import com.google.javascript.jscomp.ErrorManager;
import com.google.javascript.jscomp.GoogleCodingConvention;
import com.google.javascript.jscomp.PrintStreamErrorManager;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.head.ast.AstRoot;
import com.google.javascript.rhino.jstype.JSTypeRegistry;


public class GetImplicitDependenciesFromNodeTest {

    private static class TestCompiler extends Compiler{
        private TestCompiler() {}
        
        
        private static CompilerOptions getOptions() {
            CompilerOptions options = new CompilerOptions();
            options.setLanguageIn(LanguageMode.ECMASCRIPT5);
            options.setWarningLevel(
                DiagnosticGroups.MISSING_PROPERTIES, CheckLevel.WARNING);
            options.setCodingConvention(new GoogleCodingConvention());
            return options;
          }
        
        private static Compiler compiler;
        
        public static Compiler getCompiler() {
            if (compiler == null) {
                compiler = new Compiler();
                compiler.initOptions(getOptions());
            }
            return compiler;
        }
        
        public static Node getNodeForTest(String js) {
            Compiler comp = getCompiler();
//            JSTypeRegistry registry = comp.getTypeRegistry();
//            registry.
            try {
//                com.google.javascript.jscomp.Compiler.parseTestCode(String js)
                Method method = comp.getClass().getDeclaredMethod("parseTestCode", String.class);
                method.setAccessible(true);
                return (Node) method.invoke(comp, js);
            } catch (Exception e) {
                e.printStackTrace();
                fail("couldn't even get node");
                throw new RuntimeException(e);
            }
        }
    }
    
    

    
    @Test
    public void test() {
        
        String js = "goog.provide('Bar');\n" +
"            goog.require('Jazz');\n" +
"            /** @constructor */\n" +
"            Bar = function() {}\n;" +
"            /** @param {Foo} foo */\n" +
"            Bar.prototype.foo = function(foo) {\n" +
"            };";
        
        Node root = TestCompiler.getNodeForTest(js);
        
        Set<String> implicitDeps = new HashSet<String>();
        assertTrue(GetImplicitDependenciesNodeTraversal.run(TestCompiler.getCompiler(), root, implicitDeps));
        
        assertTrue(implicitDeps.size() == 1);
        assertTrue(implicitDeps.contains("Foo"));
    }
}
