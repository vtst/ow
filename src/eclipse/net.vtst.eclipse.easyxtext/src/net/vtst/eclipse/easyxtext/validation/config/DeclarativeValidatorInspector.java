package net.vtst.eclipse.easyxtext.validation.config;

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
    public List<Field> fields = new ArrayList<Field>(1);
  }
  
  private String propertyNameQualifier;
  private boolean enabledByDefault = true;
  private Map<String, Group> groupByName = new HashMap<String, Group>();
  private ArrayList<Group> groupList = new ArrayList<Group>();
  private Collection<Field> additionalOptionFields;
  
  /**
   * Creates a new inspector from a validator.
   * @param validator
   */
  public DeclarativeValidatorInspector(AbstractDeclarativeValidator validator) {
    inspectTypeAnnotation(validator);
    for (Method method : getCheckMethods(validator)) inspectMethod(method);
    this.additionalOptionFields = getAdditionalOptionFields(validator);
    for (Field field : this.additionalOptionFields) inspectField(field);
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
  
  /**
   * Converts a state into a boolean.
   */
  private boolean stateToBoolean(CheckState state) {
    switch (state) {
    case ENABLED: return true;
    case DISABLED: return false;
    default: return enabledByDefault;
    }
  }
  
  /**
   * Inspect the type annotation of the validator class (if any).
   * @param validator
   */
  private void inspectTypeAnnotation(AbstractDeclarativeValidator validator) {
    ConfigurableValidator annotation = validator.getClass().getAnnotation(ConfigurableValidator.class);
    if (annotation == null) return;
    enabledByDefault = stateToBoolean(annotation.defaultState());
  }
  
  /**
   * Inspect a method and its annotation.
   * @param method
   */
  private void inspectMethod(Method method) {
    ConfigurableCheck annotation = method.getAnnotation(ConfigurableCheck.class);
    Group group = inspectAnnotation(annotation, method.getName());
    if (group != null) group.methods.add(method);
  }
  
  /**
   * Inspect a field and its annotation.
   * @param field
   */
  private void inspectField(Field field) {
    ConfigurableCheck annotation = field.getAnnotation(ConfigurableCheck.class);
    if (annotation == null) return;
    Group group = inspectAnnotation(annotation, field.getName());
    if (group != null) group.fields.add(field);
  }
  
  private Group inspectAnnotation(ConfigurableCheck annotation, String name) {
    String groupName = name;
    if (annotation != null && !annotation.configurable()) return null;
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
    if (annotation != null) {
      if (annotation.defaultState() != CheckState.DEFAULT)
        group.enabledByDefault = stateToBoolean(annotation.defaultState());
      if (!annotation.group().isEmpty()) group.name = annotation.group();
      if (!annotation.label().isEmpty()) group.label = annotation.label();
    }
    return group;
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
  
  private Collection<Field> getAdditionalOptionFields(AbstractDeclarativeValidator validator) {
    ArrayList<Field> result = new ArrayList<Field>();
    for (Field field : validator.getClass().getFields()) {
      if (field.getType().equals(AdditionalBooleanOption.class)) {
        field.setAccessible(true);
        result.add(field);
      }
    }
    return result;
  }
  
  public Collection<Field> getAdditionalOptionFields() {
    return additionalOptionFields;
  }

  // **************************************************************************
  // Project properties
  
  /**
   * Get the qualified name for the resource property for a given group.
   * @param group
   * @return
   */
  private QualifiedName getQualifiedName(Group group) {
    return new QualifiedName(propertyNameQualifier, group.name);
  }
  
  /**
   * Lookup the properties of a resource, and determine whether the group is enabled.
   * @param resource
   * @param group
   * @return
   * @throws CoreException
   */
  public boolean getEnabled(IResource resource, Group group) throws CoreException {
    String value = resource.getPersistentProperty(getQualifiedName(group));
    if (value == null) return group.enabledByDefault;
    return Boolean.parseBoolean(value);
  }
  
  /**
   * Set the property of a resource for a group.
   * @param resource
   * @param group
   * @param enabled
   * @throws CoreException
   */
  public void setEnabled(IResource resource, Group group, boolean enabled) throws CoreException {
    resource.setPersistentProperty(getQualifiedName(group), Boolean.toString(enabled));
  }
  
  /**
   * @param resource
   * @return true if {@code resource} has one or several properties for
   * configuring the validator.
   * @throws CoreException
   */
  public boolean hasProperty(IResource resource) throws CoreException {
    for (QualifiedName name : resource.getPersistentProperties().keySet()) {
      if (name.getQualifier().equals(propertyNameQualifier))
        return true;
    }
    return false;
  }
  
  /**
   * Deletes all properties configuring the validator for the validator.
   * @param resource
   * @throws CoreException
   */
  public void clearAllProperties(IResource resource) throws CoreException {
    for (QualifiedName name : resource.getPersistentProperties().keySet()) {
      if (name.getQualifier().equals(propertyNameQualifier))
        resource.setPersistentProperty(name, null);
    }
  }
}
