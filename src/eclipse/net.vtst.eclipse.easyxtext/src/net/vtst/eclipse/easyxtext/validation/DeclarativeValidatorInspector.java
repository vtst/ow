package net.vtst.eclipse.easyxtext.validation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.xtext.validation.AbstractDeclarativeValidator;

/**
 * A class for inspecting the annotations of an AbstractDeclarativeValidator, and
 * accessing the corresponding project properties.
 * 
 * @author Vincent Simonet
 */
public class DeclarativeValidatorInspector {
  
  /**
   * Information about a configuration group.
   */
  public static class Group {
    public String name;
    public String label = null;
    public boolean enabledByDefault;
    public List<Method> methods = new ArrayList<Method>(1);
  }
  
  private String propertyNameQualifier;
  private boolean enabledByDefault = true;
  private Map<String, Group> groupByName = new HashMap<String, Group>();
  private ArrayList<Group> groupList = new ArrayList<Group>();

  /**
   * Creates a new inspector from a validator.
   * @param validator
   */
  public DeclarativeValidatorInspector(AbstractDeclarativeValidator validator) {
    inspectTypeAnnotation(validator);
    addMethods(getCheckMethods(validator));
    propertyNameQualifier = validator.getClass().getName();
  }

  /**
   * @return The list of configuration groups for the validator.
   */
  public ArrayList<Group> getGroups() {
    return groupList;
  }
  
  // **************************************************************************
  // Inspecting annotations
  
  private boolean stateToBoolean(CheckState state) {
    switch (state) {
    case ENABLED: return true;
    case DISABLED: return false;
    default: return enabledByDefault;
    }
  }
  
  /**
   * Inspect the type annotation of the validator class (if any).
   */
  private void inspectTypeAnnotation(AbstractDeclarativeValidator validator) {
    ConfigurableValidator annotation = validator.getClass().getAnnotation(ConfigurableValidator.class);
    if (annotation == null) return;
    enabledByDefault = stateToBoolean(annotation.defaultState());
  }
  
  private void addMethods(Collection<Method> methods) {
    for (Method method : methods)
      addMethod(method);
  }
  
  private void addMethod(Method method) {
    ConfigurableCheck annotation = method.getAnnotation(ConfigurableCheck.class);
    String groupName = method.getName();
    if (annotation != null && !annotation.configurable()) return;
    if (annotation != null && !annotation.group().isEmpty()) {
      groupName = annotation.group();
    }
    Group group = groupByName.get(groupName);
    if (group == null) {
      group = new Group();
      group.name = groupName;
      group.enabledByDefault = enabledByDefault;
      groupByName.put(groupName, group);
      groupList.add(group);
    }
    group.methods.add(method);
    if (annotation != null) {
      if (annotation.defaultState() != CheckState.DEFAULT)
        group.enabledByDefault = stateToBoolean(annotation.defaultState());
      if (!annotation.group().isEmpty()) group.name = annotation.group();
      if (!annotation.label().isEmpty()) group.label = annotation.label();
    }
  }
  
  /**
   * This is an unsafe method which access private methods and fields from AbstractDeclarativeValidator.
   * @return The collection of the methods of the validator that are annotated
   * as checks.
   */
  private Collection<Method> getCheckMethods(AbstractDeclarativeValidator validator) {
    try {
      Method collectMethodsMethod = AbstractDeclarativeValidator.class.getDeclaredMethod("collectMethods", Class.class);
      collectMethodsMethod.setAccessible(true);
      List<?> methodWrappers = (List<?>) collectMethodsMethod.invoke(validator, validator.getClass());
      ArrayList<Method> result = new ArrayList<Method>(methodWrappers.size());
      Field methodField = null;
      for (Object methodWrapper : methodWrappers) {
        if (methodField == null) {
          methodField = methodWrapper.getClass().getDeclaredField("method");
          methodField.setAccessible(true);
        }
        Method method = (Method) methodField.get(methodWrapper);
        result.add(method);
      }
      return result;
    } catch (SecurityException e) {
      // This should never happen.
      e.printStackTrace();
    } catch (NoSuchFieldException e) {
      // This should never happen.
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      // This should never happen.
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      // This should never happen.
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      // This should never happen.
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      // This should never happen.
      e.printStackTrace();
    }
    return Collections.emptyList();
  }

  // **************************************************************************
  // Project properties
  
  private QualifiedName getQualifiedName(Group group) {
    return new QualifiedName(propertyNameQualifier, group.name);
  }
  
  public boolean getEnabled(IResource resource, Group group) throws CoreException {
    String value = resource.getPersistentProperty(getQualifiedName(group));
    if (value == null) return group.enabledByDefault;
    return Boolean.parseBoolean(value);
  }
  
  public void setEnabled(IResource resource, Group group, boolean enabled) throws CoreException {
    resource.setPersistentProperty(getQualifiedName(group), Boolean.toString(enabled));
  }
  
  public boolean hasProperty(IResource resource) throws CoreException {
    for (QualifiedName name : resource.getPersistentProperties().keySet()) {
      if (name.getQualifier().equals(propertyNameQualifier))
        return true;
    }
    return false;
  }
  
  public void clearAllProperties(IResource resource) throws CoreException {
    for (QualifiedName name : resource.getPersistentProperties().keySet()) {
      if (name.getQualifier().equals(propertyNameQualifier))
        resource.setPersistentProperty(name, null);
    }
  }

}
