package net.vtst.eclipse.easyxtext.ui.editor.autoedit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.SortingFocusTraversalPolicy;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.ui.editor.autoedit.AbstractEditStrategyProvider;

import com.google.inject.Inject;

public class EasyEditStrategyProvider extends AbstractEditStrategyProvider {
  
  @Inject
  private EPackage ePackage;

  public String getPreferenceId(Method method) {
    return "EasyEditStrategyProvider:" + ePackage.getName() + ":" + method.getName();
  }
  
  public String getMessageKey(Method method) {
    return method.getDeclaringClass().getSimpleName() + "_" + method.getName();
  }
  
  IPreferenceStore store;
  
  @SuppressWarnings("deprecation")
  private IPreferenceStore getStore() {
    if (store == null) store = PlatformUI.getWorkbench().getPreferenceStore();
    return store;
  }
  
  public boolean getMethodPreferenceValue(Method method) {
    String id = getPreferenceId(method);
    ConfigureAutoEdit annotation = method.getAnnotation(ConfigureAutoEdit.class);
    assert annotation != null;
    getStore().setDefault(id, annotation.defaultState());
    return getStore().getBoolean(id);
  }
  
  public void setMethodPreferenceValue(Method method, boolean value) {
    getStore().setValue(getPreferenceId(method), value);
  }
  
  public void resetPreferenceValue(Method method) {
    getStore().setToDefault(getPreferenceId(method));    
  }

  public ArrayList<Method> getConfigureMethods() {
    ArrayList<Method> methods = new ArrayList<Method>();
    Class<?> cls = this.getClass();
    while (cls != null) {
      for (Method method: cls.getDeclaredMethods()) {
        ConfigureAutoEdit annotation = method.getAnnotation(ConfigureAutoEdit.class);
        if (annotation != null && annotation.configurable()) {
          methods.add(method);
        }
      }      
      cls = cls.getSuperclass();
    }
    sortMethods(methods);
    return methods;
  }
  
  private static void sortMethods(List<Method> methods) {
    Collections.sort(methods, new Comparator<Method>() {
      @Override
      public int compare(Method method1, Method method2) {
        ConfigureAutoEdit annot1 = method1.getAnnotation(ConfigureAutoEdit.class);
        ConfigureAutoEdit annot2 = method2.getAnnotation(ConfigureAutoEdit.class);
        int delta = annot1.order() - annot2.order();
        if (delta != 0) return delta;
        else return method1.getName().compareTo(method2.getName());
      }
    });;
  }
  
  @Override
  protected void configure(IEditStrategyAcceptor acceptor) {
    for (Method method: getConfigureMethods()) {
      if (getMethodPreferenceValue(method)) {
        method.setAccessible(true);
        try {
          method.invoke(this, acceptor);
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }
      }
    }
  }

}
