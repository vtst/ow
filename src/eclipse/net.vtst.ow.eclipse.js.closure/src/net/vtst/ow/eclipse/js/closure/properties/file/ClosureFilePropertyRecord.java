package net.vtst.ow.eclipse.js.closure.properties.file;

import net.vtst.eclipse.easy.ui.properties.Record;
import net.vtst.eclipse.easy.ui.properties.fields.BooleanField;

public class ClosureFilePropertyRecord extends Record {
  
  public ClosureFilePropertyRecord() {
    super.initializeByReflection();
  }

  public BooleanField generatedByCompiler = new BooleanField(false);

  private static ClosureFilePropertyRecord instance;
  public static ClosureFilePropertyRecord getInstance() {
    if (instance == null) instance = new ClosureFilePropertyRecord();
    return instance;
  }
}
