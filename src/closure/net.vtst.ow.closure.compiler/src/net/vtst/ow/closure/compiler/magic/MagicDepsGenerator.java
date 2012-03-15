package net.vtst.ow.closure.compiler.magic;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import com.google.javascript.jscomp.deps.DependencyInfo;
import com.google.javascript.jscomp.deps.DepsGenerator;

public class MagicDepsGenerator {
  
  private DepsGenerator depsGenerator;
  private Method writeDepInfosMethod;

  public MagicDepsGenerator() {
    depsGenerator = new DepsGenerator(null, null, null, null, null);
    writeDepInfosMethod = Magic.getDeclaredMethod(DepsGenerator.class, "writeDepInfos", PrintStream.class, Collection.class);
  }
  
  public void writeDepInfos(PrintStream out, Collection<? extends DependencyInfo> depInfos) throws IOException {
    try {
      writeDepInfosMethod.invoke(depsGenerator, out, depInfos);
    } catch (IllegalArgumentException e) {
      assert false;
    } catch (IllegalAccessException e) {
      assert false;
    } catch (InvocationTargetException e) {
      Throwable throwable = e.getTargetException();
      if (throwable instanceof IOException) {
        throw (IOException) throwable;
      }
    }
  }

}
