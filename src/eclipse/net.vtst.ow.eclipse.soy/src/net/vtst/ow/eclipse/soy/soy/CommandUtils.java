package net.vtst.ow.eclipse.soy.soy;

import java.util.NoSuchElementException;

import org.eclipse.emf.ecore.EObject;

public class CommandUtils {
  
  public static class Iterator implements Iterable<EObject>, java.util.Iterator<EObject> {
    java.util.Iterator<EObject> iterator;
    Command nextCommand = null;
    public Iterator(Command command) {
      iterator = command.eContents().iterator();
      goToNext();
    }
    
    public java.util.Iterator<EObject> iterator() {
      return this;
    }

    public boolean hasNext() {
      return (nextCommand != null);
    }

    public EObject next() {
      Command result = nextCommand;
      if (result == null) throw new NoSuchElementException();
      goToNext();
      return result;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
    
    private void goToNext() {
      while (nextCommand == null && iterator.hasNext()) {
        EObject next = iterator.next();
        if (next instanceof Command) nextCommand = (Command) next;
      }
    }
  }

  public static Iterator iterator(Command command) {
    return new Iterator(command);
  }
}
