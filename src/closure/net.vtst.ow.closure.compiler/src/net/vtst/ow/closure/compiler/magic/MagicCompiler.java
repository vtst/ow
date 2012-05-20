package net.vtst.ow.closure.compiler.magic;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerInput;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSModule;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;

/**
 * Wrapper around {@code Compiler}, in order to run a compilation by providing ASTs for externs
 * instead of source files.
 * @author Vincent Simonet
 */
public class MagicCompiler {

  private Compiler compiler;
  private Method compiler_compile;
  private Field compiler_externs;
  private Field compiler_modules;

  public MagicCompiler(Compiler compiler) {
    this.compiler = compiler;
    this.compiler_compile = Magic.getDeclaredMethod(Compiler.class, "compile");
    this.compiler_externs = Magic.getDeclaredField(Compiler.class, "externs");
    this.compiler_modules = Magic.getDeclaredField(Compiler.class, "modules");
  }
  
  // **************************************************************************
  // This is the pure adaptation of the methods of the Compiler class
  
  private <T extends SourceFile> List<CompilerInput> makeCompilerInput(
      List<T> files, boolean isExtern) {
    List<CompilerInput> inputs = Lists.newArrayList();
    for (T file : files) {
      inputs.add(new CompilerInput(file, isExtern));
    }
    return inputs;
  }

  public <T extends SourceFile> void initModules(
      List<T> externs, List<JSModule> modules, CompilerOptions options) {
    compiler.initOptions(options);

    // This is not implemented, as it is just a check.
    // compiler.checkFirstModule(modules);
    // This is not implemented, as it only applies to empty modules.
    // compiler.fillEmptyModules(modules);

    try {
      compiler_externs.set(compiler, makeCompilerInput(externs, true));

      compiler_modules.set(compiler, modules);
      // This is not implemented, as it applies only if there is more than on
      // module.
      // if (modules.size() > 1) {
      //   try {
      //     this.moduleGraph = new JSModuleGraph(modules);
      //   } catch (JSModuleGraph.ModuleDependenceException e) {
      //     // problems with the module format.  Report as an error.  The
      //     // message gives all details.
      //     report(JSError.make(MODULE_DEPENDENCY_ERROR,
      //             e.getModule().getName(), e.getDependentModule().getName()));
      //     return;
      //   }
      // } else {
      //   this.moduleGraph = null;
      // }
      // This call initBasedOnOptions and initInputsByIdMap.
      compiler.rebuildInputsFromModules();
      // this.inputs = Compiler.getAllInputsFromModules(modules);
      // This is not useful for the options we run.
      // compiler.initBasedOnOptions();
  
      // compiler.initInputsByIdMap();
    } catch (IllegalArgumentException e) {
      throw new MagicException(e);
    } catch (IllegalAccessException e) {
      throw new MagicException(e);      
    }
  }

  public <T extends SourceFile> Result compileModules(List<T> externs, List<JSModule> modules, CompilerOptions options) {
    // The compile method should only be called once.
    Preconditions.checkState(compiler.getRoot() == null);

    try {
      initModules(externs, modules, options);
      if (compiler.hasErrors()) {
        return compiler.getResult();
      }
      return compile();
    } finally {
      compiler.getErrorManager().generateReport();
    }
  }
  
  private Result compile() {
    try {
      Object result = compiler_compile.invoke(compiler);
      if (result instanceof Result) return (Result) result;
      return null;
    } catch (IllegalArgumentException e) {
      throw new MagicException(e);
    } catch (IllegalAccessException e) {
      throw new MagicException(e);
    } catch (InvocationTargetException e) {
      Magic.catchInvocationTargetException(e);
      return null;
    }
  }

  // **************************************************************************
  // These are the new methods.

  private void init(List<CompilerInput> externs, JSModule module, CompilerOptions options) {
    compiler.initOptions(options);
    try {
      compiler_externs.set(compiler, externs);
      compiler_modules.set(compiler, Lists.newArrayList(module));
      compiler.rebuildInputsFromModules();
    } catch (IllegalArgumentException e) {
      throw new MagicException(e);
    } catch (IllegalAccessException e) {
      throw new MagicException(e);      
    }    
  }
  
  public Result compile(List<CompilerInput> externs, JSModule module, CompilerOptions options) {
    // The compile method should only be called once.
    Preconditions.checkState(compiler.getRoot() == null);

    try {
      init(externs, module, options);
      if (compiler.hasErrors()) {
        return compiler.getResult();
      }
      return compile();
    } finally {
      compiler.getErrorManager().generateReport();
    }
  }

  public static Result compile(Compiler compiler, List<CompilerInput> externs, JSModule module, CompilerOptions options) {
    MagicCompiler magicCompiler = new MagicCompiler(compiler);
    return magicCompiler.compile(externs, module, options);
  }
  
}
