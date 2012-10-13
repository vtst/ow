package net.vtst.ow.eclipse.js.closure.preferences;

import net.vtst.eclipse.easy.ui.properties.Record;
import net.vtst.eclipse.easy.ui.properties.fields.BooleanField;
import net.vtst.eclipse.easy.ui.properties.fields.EnumOptionsField;
import net.vtst.eclipse.easy.ui.properties.fields.FileField;
import net.vtst.ow.closure.compiler.deps.JSLibrary.CacheMode;

public class ClosurePreferenceRecord extends Record {

  public ClosurePreferenceRecord() {
    super.initializeByReflection();
  }
  
  public FileField closureBasePath = new FileField(null, FileField.Type.DIRECTORY);
  public EnumOptionsField<CacheMode> cacheLibraryStrippedFiles = 
      new EnumOptionsField<CacheMode>(CacheMode.class, CacheMode.READ_AND_WRITE);
  public EnumOptionsField<CacheMode> cacheLibraryDepsFiles = 
      new EnumOptionsField<CacheMode>(CacheMode.class, CacheMode.READ_AND_WRITE);
  public BooleanField stripProjectFiles = new BooleanField(true);
  public BooleanField doNotKeepCompilationResultsOfClosedFilesInMemory = new BooleanField(false);
  public BooleanField doNotCompileClosedFiles = new BooleanField(false);

  private static ClosurePreferenceRecord instance;
  public static ClosurePreferenceRecord getInstance() {
    if (instance == null) instance = new ClosurePreferenceRecord();
    return instance;
  }
}
