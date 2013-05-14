package net.vtst.ow.eclipse.less.scoping;

import java.util.ArrayList;

import net.vtst.ow.eclipse.less.less.HashOrClassRef;
import net.vtst.ow.eclipse.less.less.MixinUtils;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;

/**
 * An element of a mixin scope.  It can be converted into an object description.
 * 
 * @author Vincent Simonet
 */
public class MixinScopeElement {
  
  private String name;
  private ArrayList<EObject> objects;
  
  /**
   * Create a root element. 
   */
  public MixinScopeElement() {
    this(null, new ArrayList<EObject>());
  }
  
  /**
   * Create a non-root element.
   * @param name  The name of the element.
   * @param objects  The list of hashOrClass that point to this element.
   */
  private MixinScopeElement(String name, ArrayList<EObject> objects) {
    this.name = name;
    this.objects = objects;
  }
  
  /**
   * @return  The name of the element.
   */
  public String getName() { return name; }
    
  /**
   * @param index
   * @return The hashOrClass at the index-th position.
   */
  public EObject getObject(int index) { return objects.get(index); }

  
  public EObject getLastObject() {
    if (objects.size() == 0) return null;
    return objects.get(objects.size() - 1);
  }

  /**
   * Create a new element, with the same list of objects + one object.
   * @param name
   * @param pushedObject
   * @return  The new element.
   */
  public MixinScopeElement cloneAndExtends(String name, EObject pushedObject) {
    @SuppressWarnings("unchecked")
    ArrayList<EObject> newObjects = (ArrayList<EObject>) this.objects.clone();
    newObjects.add(pushedObject);
    return new MixinScopeElement(name, newObjects);
  }

  /**
   * @return The qualified name for the current element.
   */
  private QualifiedName getQualifiedName() {
    if (this.name.startsWith(".")) {
      // This is because of the special interpretation of '.' by xtext when doing linking.
      // TODO A better approach would be to de-activate the special handling of . by implementing
      // IQualifiedNameConverter.
      return QualifiedName.create("", this.name.substring(1));
    } else {
      return QualifiedName.create(this.name);
    }
  }
  
  /**
   * Converts the element into an object description.
   * @param index
   * @return
   */
  public IEObjectDescription asEObjectDescription(int index) {
    return EObjectDescription.create(getQualifiedName(), getObject(index));
  }
  
}
