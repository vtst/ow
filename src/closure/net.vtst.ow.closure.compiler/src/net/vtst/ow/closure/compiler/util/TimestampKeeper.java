package net.vtst.ow.closure.compiler.util;

import net.vtst.ow.closure.compiler.deps.JSUnitProvider;

public class TimestampKeeper {
  
  private JSUnitProvider.IProvider provider;
  private long lastModified = -1;

  public TimestampKeeper(JSUnitProvider.IProvider provider) {
    this.provider = provider;
  }
  
  /**
   * @return true if the source has changed since the last call.
   */
  public boolean hasChanged() {
    long newLastModified = provider.lastModified();
    if (newLastModified > lastModified) {
      lastModified = newLastModified;
      return true;
    } else {
      return false;
    }    
  }
  
  /**
   * Sync the internal timestamp with the current one.
   */
  public void sync() {
    long newLastModified = provider.lastModified();
    if (newLastModified > lastModified) lastModified = newLastModified;    
  }

  public long getModificationStamp() {
    return lastModified;
  }

}
