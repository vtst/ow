package net.vtst.ow.closure.compiler.deps;

import java.util.List;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.deps.SortedDependencies.CircularDependencyException;

/**
 * Concrete implementation of a compilation set, which may own some compilation unit, and contain
 * some delegated compilation sets.
 * <br>
 * <b>Thread safety:</b>  This class is not thread safe.  It can be accessed by only one thread.
 * @author Vincent Simonet
 */
public class JSProject extends AbstractJSProject {
  

  public <T extends JSUnit> void setUnits(AbstractCompiler compiler, List<T> units) throws CircularDependencyException {
    for (JSUnit unit: units) {
      unit.updateDependencies(compiler);
    }
    super.setUnits(compiler, units);
  }
  // **************************************************************************
  // Referenced projects

  private List<AbstractJSProject> referencedProjects;
  
  /**
   * Add a delegated compilation set.  Does not recompute dependencies.
   * @param compilationSet  The delegated compilation set.
   */
  public void setReferencedProjects(List<AbstractJSProject> referencedProjects) {
    this.referencedProjects = referencedProjects;
  }
  
  @Override
  protected List<AbstractJSProject> getReferencedProjects() {
    return referencedProjects;
  }
}
