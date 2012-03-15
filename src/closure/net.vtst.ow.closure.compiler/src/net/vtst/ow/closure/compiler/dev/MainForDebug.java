package net.vtst.ow.closure.compiler.dev;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;

import net.vtst.ow.closure.compiler.deps.AstFactory;
import net.vtst.ow.closure.compiler.deps.JSLibrary;
import net.vtst.ow.closure.compiler.deps.JSSet;
import net.vtst.ow.closure.compiler.deps.JSUnit;
import net.vtst.ow.closure.compiler.deps.JSUnitProvider;
import net.vtst.ow.closure.compiler.util.CompilerUtils;
import net.vtst.ow.closure.compiler.util.FileTreeVisitor;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerInput;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerPass;
import com.google.javascript.jscomp.DefaultPassConfig;
import com.google.javascript.jscomp.JSModule;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.PassConfig;
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
    final ArrayList<AstFactory> listAsts = new ArrayList<AstFactory>(); 
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
      for (AstFactory ast: listAsts) {
        module.add(new CompilerInput(ast.getClone()));
      }
//      for (JSSourceFile sourceFile: listSourceFiles) {
//        module.add(new CompilerInput(sourceFile));
//      }
//      for (File file: listFiles) {
//        module.add(new CompilerInput(JSSourceFile.fromFile(file)));
//      }
      Compiler compiler = CompilerUtils.makeCompiler(CompilerUtils.makePrintingErrorManager(System.out));
      CompilerOptions options = CompilerUtils.makeOptions();
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
    File pathOfClosureBase = new File("/home/vtst/test/in/closure/goog");
    Compiler.setLoggingLevel(Level.OFF);
    Compiler compiler = CompilerUtils.makeCompiler(CompilerUtils.makePrintingErrorManager(System.out));
    CompilerOptions options = CompilerUtils.makeOptions();
    options.checkTypes = true;
    compiler.initOptions(options);
    PassConfig passes = new DefaultPassConfig(options);
    compiler.setPassConfig(passes);
    JSSet compilationSet = new JSSet();
    JSLibrary libraryGoog = new JSLibrary(pathOfClosureBase);
    libraryGoog.updateDependencies(compiler);
    compilationSet.addCompilationSet(libraryGoog);
    JSLibrary librarySoy = new JSLibrary(new File("/home/vtst/perso/ow/tgt/closure-templates/javascript"));
    librarySoy.updateDependencies(compiler);
    compilationSet.addCompilationSet(librarySoy);
    ArrayList<JSUnit> compilationUnits = new ArrayList<JSUnit>();
    for (String name: new String[]{
        "album.js", "header.js", "master.js", "splitpane.js", "util.js",
        "browser.js", "keyboard.js", "sepia.js", "templates.js", "viewer.js" 
    }) {
      File path = new File("/home/vtst/perso/sepia/src/client/js/" + name);
      JSUnit compilationUnit = new JSUnit(path, pathOfClosureBase, new JSUnitProvider.FromFile(path));
      compilationSet.addCompilationUnit(compilationUnit);
      compilationUnits.add(compilationUnit);
    }
    compilationSet.updateDependencies(compiler);
    long t0 = System.nanoTime();
    for (int i = 0; i < 1; i++) {
      Compiler compiler2 = CompilerUtils.makeCompiler(CompilerUtils.makePrintingErrorManager(System.out));
      CompilerOptions options2 = CompilerUtils.makeOptions();
      options2.checkTypes = true;
      compiler.initOptions(options2);
      PassConfig passes2 = new DefaultPassConfig(options2);
      compiler2.setPassConfig(passes2);
      JSModule module = compilationSet.makeJSModule(compiler2, "test-module", compilationUnits);
      compiler2.compile(new JSSourceFile[]{}, new JSModule[]{module}, options2);
      System.out.println((System.nanoTime() - t0) * 1e-9);
    }
    //System.out.println(compiler.toSource());
    //System.out.println(compiler.getRoot().toStringTree());
    //DevUtils.printNodeAsTree(compiler, compiler.getRoot());
    /*
    System.out.println("YOP1");
    Node node = FindLocationNodeTraversal.find(compiler, compiler.getRoot(), "test.js", 514);
    System.out.println("YOP2");
    Iterable<String> proposals = ContentAssist.getContentProposals(compiler, passes, node, "");
    System.out.println("YOP3");
    for (String proposal: proposals) {
      System.out.println(proposal);
    }
    */
    //compiler.getErrorManager().generateReport();
    System.out.println("Done.");
  }
  
  public static void compile(JSModule module) {
    Compiler compiler = CompilerUtils.makeCompiler(CompilerUtils.makePrintingErrorManager(System.out));    
    CompilerOptions options = CompilerUtils.makeOptions();
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
    File pathOfClosureBase = new File("/home/vtst/test/in/closure/goog");
    JSLibrary library = new JSLibrary(pathOfClosureBase);
    Compiler compiler = CompilerUtils.makeCompiler(CompilerUtils.makePrintingErrorManager(System.out));
    compiler.initOptions(CompilerUtils.makeOptions());
    library.updateDependencies(compiler);
    System.out.println("FINISHED");
  }
  
  public static void main(String[] args) {
    testDeps();
  }

}
