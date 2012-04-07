package net.vtst.ow.eclipse.js.closure.preferences;

import net.vtst.eclipse.easy.ui.properties.Record;
import net.vtst.eclipse.easy.ui.properties.fields.BooleanField;
import net.vtst.eclipse.easy.ui.properties.fields.FileField;

public class ClosurePreferenceRecord extends Record {

  public ClosurePreferenceRecord() {
    super.initializeByReflection();
  }
  
  public FileField closureBasePath = new FileField("", FileField.Type.DIRECTORY);
  public BooleanField readStrippedLibraryFiles = new BooleanField(true);
  public BooleanField writeStrippedLibraryFiles = new BooleanField(false);

}
