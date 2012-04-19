package net.vtst.ow.closure.compiler.deps;

import java.util.List;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.deps.SortedDependencies.CircularDependencyException;

/**
 * Concrete implementation of a compilation set, which may own some compilation unit, and contain
 * some delegated compilation sets.
 * <br>
 * <b>Thread safety:</b>  This class is not thread safe.  Only the sub-class {@code AbstractJSProject}
 * needs to be thread safe.
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

  private long referencedProjectsModificationStamp = 0;
  private List<AbstractJSProject> referencedProjects;
  private List<JSExtern> externs;
  
  /**
   * Set the projects referenced by a project.
   * @param compilationSet  The referenced projects.
   */
  public void setReferencedProjects(List<AbstractJSProject> referencedProjects) {
    setReferencedProjects(referencedProjects, 0);
  }
  
  /**
   * Set the projects referenced by a project.
   * @param referencedProjects  The referenced projects.
   * @param modificationStamp  The associated modification stamp.
   */
  public void setReferencedProjects(List<AbstractJSProject> referencedProjects, long modificationStamp) {
    this.referencedProjects = referencedProjects;
    this.referencedProjectsModificationStamp = modificationStamp;
  }
  
  /**
   * @return  The modification stamp for the referenced projects.
   */
  public long getReferencedProjectsModificationStamp() {
    return referencedProjectsModificationStamp;
  }
  
  /* (non-Javadoc)
   * @see net.vtst.ow.closure.compiler.deps.AbstractJSProject#getReferencedProjects()
   */
  @Override
  protected List<AbstractJSProject> getReferencedProjects() {
    return referencedProjects;
  }
  
  public void setExterns(List<JSExtern> externs) {
    this.externs = externs;
  }

  public List<JSExtern> getExterns() {
    return this.externs;
  }
}
