package net.vtst.ow.eclipse.less.scoping;

import java.util.ArrayList;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;

public class MixinScopeElement {
  
  private String name;
  private ArrayList<EObject> objects;
  
  public MixinScopeElement() {
    this(null, new ArrayList<EObject>());
  }
  
  private MixinScopeElement(String name, ArrayList<EObject> objects) {
    this.name = name;
    this.objects = objects;
  }
  
  public String getName() { return name; }
  
  public EObject getObject(int index) { return objects.get(index); }
  
  public MixinScopeElement cloneAndExtends(String name, EObject pushedObject) {
    @SuppressWarnings("unchecked")
    ArrayList<EObject> newObjects = (ArrayList<EObject>) this.objects.clone();
    newObjects.add(pushedObject);
    return new MixinScopeElement(name, newObjects);
  }

  private QualifiedName asQualifiedName() {
    if (this.name.startsWith(".")) {
      // This is because of the special interpretation of '.' by xtext when doing linking.
      // TODO A better approach would be to de-activate the special handling of . by implementing
      // IQualifiedNameConverter.
      return QualifiedName.create("", this.name.substring(1));
    } else {
      return QualifiedName.create(this.name);
    }
  }
  
  public IEObjectDescription asEObjectDescription(int index) {
    return EObjectDescription.create(asQualifiedName(), getObject(index));
  }
  
}
