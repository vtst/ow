package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.eclipse.easy.ui.properties.Record;
import net.vtst.eclipse.easy.ui.properties.fields.BooleanField;
import net.vtst.eclipse.easy.ui.properties.fields.FileField;
import net.vtst.eclipse.easy.ui.properties.fields.FileListField;

public class ClosureProjectPropertyRecord extends Record {
  
  public ClosureProjectPropertyRecord() {
    super.initializeByReflection();
  }
  
  public BooleanField useDefaultClosureBasePath = new BooleanField(true);
  public FileField closureBasePath = new FileField("", FileField.Type.DIRECTORY);
  public FileListField otherLibraries = new FileListField(FileListField.Type.DIRECTORY);
  
  private static ClosureProjectPropertyRecord instance;
  public static ClosureProjectPropertyRecord getInstance() {
    if (instance == null) instance = new ClosureProjectPropertyRecord();
    return instance;
  }
  
}
