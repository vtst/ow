package net.vtst.eclipse.easy.ui.properties;

import java.lang.reflect.Field;

import net.vtst.eclipse.easy.ui.properties.fields.IField;

/**
 * This class can be used for creating place holder for fields.  Sub-classes must call
 * {@code initializeByReflection} at initialization.
 */
public class Record {
  public final void initializeByReflection() {
    for (Field field: this.getClass().getFields()) {
      try {
        Object fieldValue = field.get(this);
        if (fieldValue instanceof IField) {
          ((IField<?>) fieldValue).bind(field.getName());
        } else if (fieldValue instanceof Record) {
          ((Record) fieldValue).initializeByReflection();
        }
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
  }
}
