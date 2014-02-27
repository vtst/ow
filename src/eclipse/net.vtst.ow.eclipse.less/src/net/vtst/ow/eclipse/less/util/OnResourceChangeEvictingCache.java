package net.vtst.ow.eclipse.less.util;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class OnResourceChangeEvictingCache implements IOnResourceChangeEvictingCache {
  
  private final Map<Object, MapValue> values = new ConcurrentHashMap<Object, MapValue>(500);
 
  // TODO: Clarify the semantics of resource.

  public <T> T get(Object key, Resource resource, Provider<T> provider) {
    MapValue mapValue = values.get(key);
    if (mapValue == null) {
      T newValue = provider.get();
      this.putInternal(key, resource, newValue);
      return newValue;
    } else {
      @SuppressWarnings("unchecked")
      T existingValue = (T) mapValue.value;
      return (T) existingValue;
    }
  }

  public <T> void put(Object key, Resource resource, T value) {
    MapValue previousMapValue = this.putInternal(key, resource, value);
    if (previousMapValue != null && !previousMapValue.resource.equals(resource))
      previousMapValue.adapter.removeKey(key);
  }

  private <T> MapValue putInternal(Object key, Resource resource, T value) {
    CacheAdapter adapter = this.getOrCreateAdapter(resource);
    MapValue previousMapValue = values.put(key, new MapValue(resource, adapter, value));
    adapter.addKey(key);
    return previousMapValue;
  }
  
  public void remove(Object key) {
    MapValue mapValue = this.values.remove(key);
    if (mapValue != null) mapValue.adapter.removeKey(key);
  }
  
  public CacheAdapter getOrCreateAdapter(Resource resource) {
    CacheAdapter adapter = (CacheAdapter) EcoreUtil.getAdapter(resource.eAdapters(), CacheAdapter.class);
    if (adapter == null) {
      adapter = new CacheAdapter();
      resource.eAdapters().add(adapter);
    }
    return adapter;
  }
  
  private static class MapValue {
    private Resource resource;
    private CacheAdapter adapter;
    private Object value;
    private MapValue(Resource resource, CacheAdapter adapter, Object value) {
      this.resource = resource;
      this.adapter = adapter;
      this.value = value;
    }
  }

  public class CacheAdapter extends EContentAdapter {
    
    private final Map<Object, Void> boundKeys = new ConcurrentHashMap<Object, Void>();
    
    @Override
    public void notifyChanged(Notification notification) {
      super.notifyChanged(notification);
      if (this.isSemanticStateChange(notification)) {
        for (Entry<Object, Void> entry: this.boundKeys.entrySet()) {
          // TODO: Qualify call to remove.
          remove(entry.getKey());
        }
      }
    }
    
    public void addKey(Object key) {
      this.boundKeys.put(key, null);
    }

    public void removeKey(Object key) {
      this.boundKeys.remove(key);
    }

    private boolean isSemanticStateChange(Notification notification) {
      return !notification.isTouch() &&
          !(notification.getNewValue() instanceof Diagnostic) &&
          !(notification.getOldValue() instanceof Diagnostic);
    }

    @Override
    public boolean isAdapterForType(Object type) {
      return type == getClass();
    }

    @Override
    protected boolean resolve() {
      return false;
    }

  }

}
