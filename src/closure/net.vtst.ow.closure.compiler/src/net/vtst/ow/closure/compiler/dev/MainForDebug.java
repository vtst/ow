package net.vtst.ow.closure.compiler.dev;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import net.vtst.ow.closure.compiler.deps.AstFactoryFromModifiable;
import net.vtst.ow.closure.compiler.util.CompilerUtils;
import net.vtst.ow.closure.compiler.util.FileTreeVisitor;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerInput;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerPass;
import com.google.javascript.jscomp.DefaultPassConfig;
import com.google.javascript.jscomp.JSModule;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.Scope;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.jstype.JSType;

/*
 * Some performance measurements.
 * Compilation in ideMode.
 * 10 compilations 
 *                    Original Closure Lib   Stripped Closure Lib
 * Input files        758                    758
 * Output size        4628978                2575367
 * Time 10 files
 *      File          22.56                  13.38
 *      JSSourceFile  21.69                  12.85         
 *      ast.clone()   16.19                   9.20
 * Time 9 last files
 *      File          16.59                  9.26
 *      JSSourceFile  15.73                  8.79
 *      ast.clone()   10.07                  5.25
 *      with typing                          24.23
 * Combination of all my optimizations = 4.13
 */

public class MainForDebug {
  private static Method method;
  private static DefaultPassConfig passConfig;
  
  public static void measureTime() {
    final ArrayList<File> listFiles = new ArrayList<File>();
    final ArrayList<JSSourceFile> listSourceFiles = new ArrayList<JSSourceFile>();
    final ArrayList<AstFactoryFromModifiable> listAsts = new ArrayList<AstFactoryFromModifiable>(); 
    FileTreeVisitor.Simple<RuntimeException> visitor = new FileTreeVisitor.Simple<RuntimeException>() {
      public void visitFile(File file) {
        if (!CompilerUtils.isJavaScriptFile(file)) return;
        listFiles.add(file);
        JSSourceFile sourceFile = JSSourceFile.fromFile(file);
        listSourceFiles.add(sourceFile);
        //listAsts.add(new JsAstFactoryFromFile(file));
      }
    };
    visitor.visit(new File("/home/vtst/test/out/goog"));
    System.out.println(listAsts.size());
    long t0 = System.nanoTime();
    long t1 = 0;
    for (int i = 0; i < 10; ++i) {
      if (i == 1) t1 = System.nanoTime();
      JSModule module = new JSModule("main");
      for (AstFactoryFromModifiable ast: listAsts) {
        module.add(new CompilerInput(ast.getClone(false)));
      }
//      for (JSSourceFile sourceFile: listSourceFiles) {
//        module.add(new CompilerInput(sourceFile));
//      }
//      for (File file: listFiles) {
//        module.add(new CompilerInput(JSSourceFile.fromFile(file)));
//      }
      Compiler compiler = CompilerUtils.makeCompiler(CompilerUtils.makePrintingErrorManager(System.out));
      CompilerOptions options = CompilerUtils.makeOptionsForParsingAndErrorReporting();
      options.checkTypes = true;
      compiler.compile(new JSSourceFile[]{}, new JSModule[]{module}, options);
      System.out.println(compiler.toSource().length());
    }
    long tf = System.nanoTime();
    System.out.println((tf - t0) * 1e-9);
    System.out.println((tf - t1) * 1e-9);
    System.out.println("DONE");
  }
  
  public static void testCompilationSet() {
    /*
     * Jan 31, 2012 9:43:00 PM com.google.javascript.jscomp.PhaseOptimizer$NamedPass process
INFO: checkVars
Jan 31, 2012 9:43:01 PM com.google.javascript.jscomp.PhaseOptimizer$NamedPass process
INFO: resolveTypes
Jan 31, 2012 9:43:01 PM com.google.javascript.jscomp.PhaseOptimizer$NamedPass process
INFO: inferTypes
Jan 31, 2012 9:43:01 PM com.google.javascript.jscomp.PhaseOptimizer$NamedPass process
INFO: checkTypes
Jan 31, 2012 9:43:02 PM com.google.javascript.jscomp.PhaseOptimizer$NamedPass process
INFO: checkStrictMode
Jan 31, 2012 9:43:02 PM com.google.javascript.jscomp.PhaseOptimizer$NamedPass process
INFO: replaceMessages
Jan 31, 2012 9:43:02 PM com.google.javascript.jscomp.PhaseOptimizer$NamedPass process
INFO: processDefines

     */
  }
  
  public static void compile(JSModule module) {
    Compiler compiler = CompilerUtils.makeCompiler(CompilerUtils.makePrintingErrorManager(System.out));    
    CompilerOptions options = CompilerUtils.makeOptionsForParsingAndErrorReporting();
    compiler.initOptions(options);
    
    JSSourceFile extern = JSSourceFile.fromCode("externs.js", "");
    compiler.compile(extern, new JSModule[]{module}, options);
    System.out.println(compiler.toSource());
  }
  
  private static class MyCompilerPass implements CompilerPass {

    private Compiler compiler;

    public MyCompilerPass(Compiler compiler) {
      this.compiler = compiler;
    }

    @Override
    public void process(Node externs, Node root) {
      System.out.println("VTST PASS");
      try {
        method.invoke(passConfig, compiler, root);
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      }
      CompilerInput input = compiler.getInput(root.getInputId());
      Scope scope = compiler.getTopScope();
      try {
        Method meth = Scope.class.getDeclaredMethod("declare", String.class, Node.class, JSType.class, CompilerInput.class);
        meth.setAccessible(true);
        meth.invoke(scope, "test_vtst", root, compiler.getTypeRegistry().getType("number"), input);
        System.out.println(scope.getVar("test_vtst"));
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      } catch (SecurityException e) {
        e.printStackTrace();
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      }

    }
  }
  
  private static void testDeps() {
  }
  
  public static void main(String[] args) {
    testDeps();
  }

}
