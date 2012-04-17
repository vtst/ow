package net.vtst.ow.closure.compiler.deps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.deps.SortedDependencies;
import com.google.javascript.jscomp.deps.SortedDependencies.CircularDependencyException;
import com.google.javascript.jscomp.deps.SortedDependencies.MissingProvideException;

/**
 * A super class for {@code JSLibrary} and {@code JSProject}.
 * <b>Thread safety:</b>  Thread safety is ensured for {@code setUnits} and {@code sortUnitsByDependencies}.
 * Other methods do not really need to be synchronized.
 * @author Vincent Simonet
 */
public abstract class AbstractJSProject {
  
  SortedDependencies<? extends JSUnit> dependencies;
  
  /**
   * Set the list of units for the project, and re-build the dependency graph.
   * @param units  The new list of units.
   * @throws CircularDependencyException  If there is a circular dependency in the passed list.
   */
  public synchronized <T extends JSUnit> void setUnits(AbstractCompiler compiler, List<T> units) throws CircularDependencyException {
    dependencies = new SortedDependencies<T>(units);
    int index = 0;
    for (JSUnit unit: dependencies.getSortedList()) {
      unit.dependencyIndex = index;
      ++index;
    }
  }
  
  private static Comparator<JSUnit> jsUnitComparator = new Comparator<JSUnit>() {
    @Override
    public int compare(JSUnit unit1, JSUnit unit2) {
      return (unit1.dependencyIndex - unit2.dependencyIndex);
    }
  };
  
  /**
   * Sort a list of compilation units (which should belong to the current project), according
   * to their dependency order.
   * @param units
   */
  protected synchronized void sortUnitsByDependencies(List<JSUnit> units) {
    Collections.sort(units, jsUnitComparator);
  }

  /**
   * Get the unit providing a name in the current project.  Does not perform recursive calls
   * to the referenced projects.
   * @param name  The name to look for.
   * @return  The providing unit, or {@code null}.
   */
  protected JSUnit getUnitProviding(String name) {
    if (dependencies == null) return null;
    try {
      return dependencies.getInputProviding(name);
    } catch (MissingProvideException e) {
      return null;
    }
  }

  /**
   * @return  The list of referenced projects.  The project must be ordered according to their
   * dependencies, in decreasing order: if A comes before B, A may depend on B, but B cannot 
   * depend on A.  Note there is no recursion: the referenced projects of referenced projects
   * must be included in the list if needed.
   */
  protected abstract List<AbstractJSProject> getReferencedProjects();

  /**
   * Helper class for {@code getSortedDependenciesOf}.
   */
  private class DependencyBuilder {
    private ArrayList<ArrayList<JSUnit>> results;
    private LinkedList<String> namesToVisit;
    Set<String> foundNames;

    private void init(AbstractJSProject project, Collection<String> requires, Collection<String> provides) {
      namesToVisit = Lists.newLinkedList(requires);
      foundNames = Sets.newHashSet(Iterables.concat(provides, requires));
      List<AbstractJSProject> referencedProjects = getReferencedProjects();
      results = new ArrayList<ArrayList<JSUnit>>(referencedProjects.size() + 1);
      visit(project);
      for (AbstractJSProject referencedProject: referencedProjects) {
        visit(referencedProject);
      }
    }
    
    DependencyBuilder(AbstractJSProject project, JSUnit unit) {
      init(project, unit.getRequires(), unit.getProvides());
    }
    
    DependencyBuilder(AbstractJSProject project, Iterable<JSUnit> units) {
      Collection<String> requires = new HashSet<String>();
      Collection<String> provides = new HashSet<String>();
      for (JSUnit unit: units) {
        requires.addAll(unit.getRequires());
        provides.addAll(unit.getProvides());
      }
      init(project, requires, provides);
    }

    
    void visit(AbstractJSProject project) {
      LinkedList<String> remainingNames = new LinkedList<String>();
      ArrayList<JSUnit> unitsInThisProject = new ArrayList<JSUnit>();
      while (!namesToVisit.isEmpty()) {
        String name = namesToVisit.remove();
        JSUnit providingUnit = project.getUnitProviding(name);
        if (providingUnit == null) {
          remainingNames.add(name);
        } else {
          unitsInThisProject.add(providingUnit);
          for (String requiredName: providingUnit.getRequires()) {
            if (foundNames.add(requiredName)) namesToVisit.add(requiredName);
          }
        }
      }
      namesToVisit = remainingNames;
      project.sortUnitsByDependencies(unitsInThisProject);
      results.add(unitsInThisProject);
    }
    
    ArrayList<JSUnit> get() {
      int size = 0;
      for (ArrayList<JSUnit> list: results) size += list.size();
      ArrayList<JSUnit> result = new ArrayList<JSUnit>(size);
      for (ArrayList<JSUnit> list:results) result.addAll(list);
      return result;
    }
    
  }
  
  /**
   * Returns the list of units which are required to build {@code unit}, ordered according to
   * their dependencies.  Units from referenced projects are included.
   * @param unit  The unit to look for, which must be in the list of units of the project.
   * @return  The units required to build {@code unit} ({@code unit} is not included).
   */
  public List<JSUnit> getSortedDependenciesOf(JSUnit unit) {
    DependencyBuilder builder = new DependencyBuilder(this, unit);
    return builder.get();
  }
  
  /**
   * Returns the list of units which are required to build {@code units}, ordered according to
   * their dependencies.  Units from referenced projects are included.
   * @param units  The units to look for, which must be a subset of the units of the project.
   * @return  The units required to build {@code units} ({@code units} are not included).
   */
  public List<JSUnit> getSortedDependenciesOf(Iterable<JSUnit> units) {
    DependencyBuilder builder = new DependencyBuilder(this, units);
    return builder.get();
  }


  // **************************************************************************
  // Error reporting
  
  static final DiagnosticType CIRCULAR_DEPENDENCY_ERROR =
      DiagnosticType.error("JSC_CIRCULAR_DEP",
          "Circular dependency detected: {0}");

  protected void reportError(AbstractCompiler compiler, CircularDependencyException e) {
    compiler.report(JSError.make(CIRCULAR_DEPENDENCY_ERROR, e.getMessage()));   
  }
}
