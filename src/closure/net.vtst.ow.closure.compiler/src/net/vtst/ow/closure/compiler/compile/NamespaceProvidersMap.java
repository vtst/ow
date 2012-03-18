package net.vtst.ow.closure.compiler.compile;

import java.util.HashMap;
import java.util.Map;

import net.vtst.ow.closure.compiler.util.MultiHashMap;

import com.google.javascript.rhino.InputId;
import com.google.javascript.rhino.Node;

/**
 * A map for storing the script node which provides every name space.  This is used by
 * {@code NamespaceProvidersPass}.
 * @author Vincent Simonet
 */
public class NamespaceProvidersMap {

  private Map<String, Node> namespaceToNode = new HashMap<String, Node>();
  private MultiHashMap<InputId, String> inputIdToNamespace = new MultiHashMap<InputId, String>();

  public void put(String namespace, Node scriptRoot) {
    namespaceToNode.put(namespace, scriptRoot);
    inputIdToNamespace.put(scriptRoot.getInputId(), namespace);
  }
  
  public void removeAll(Node scriptRoot) {
    for (String namespace: inputIdToNamespace.getAndRemoveAll(scriptRoot.getInputId())) {
      namespaceToNode.remove(namespace);
    }
  }
  
  public Node get(String namespace) {
    return namespaceToNode.get(namespace);
  }
  
}
