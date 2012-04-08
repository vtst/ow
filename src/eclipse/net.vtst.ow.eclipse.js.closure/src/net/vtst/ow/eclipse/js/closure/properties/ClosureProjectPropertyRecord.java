package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.eclipse.easy.ui.properties.Record;
import net.vtst.eclipse.easy.ui.properties.fields.BooleanField;
import net.vtst.eclipse.easy.ui.properties.fields.FileField;
import net.vtst.eclipse.easy.ui.properties.fields.FileListField;
import net.vtst.eclipse.easy.ui.properties.fields.StringField;
import net.vtst.eclipse.easy.ui.properties.fields.StringOptionsField;

public class ClosureProjectPropertyRecord extends Record {
  
  public ClosureProjectPropertyRecord() {
    super.initializeByReflection();
  }

  public BooleanField useDefaultClosureBasePath = new BooleanField(true);
  public FileField closureBasePath = new FileField(null, FileField.Type.DIRECTORY);

  // Includes
  
  public FileListField otherLibraries = new FileListField(FileListField.Type.DIRECTORY);
  public FileListField externs = new FileListField(FileListField.Type.FILE, 
      new String[]{"*.js", "*"}, new String[]{"js", "all"});
  public BooleanField useOnlyCustomExterns = new BooleanField(false);
  
  // Compilation (checks)
  
  public StringOptionsField warningLevel = new StringOptionsField("QUIET", "DEFAULT", "VERBOSE");
  public BooleanField thirdParty = new BooleanField(false);
  public BooleanField processClosurePrimitives = new BooleanField(true);
  public StringField closureEntryPoints = new StringField("");
  public BooleanField processJQueryPrimitives = new BooleanField(false);
  public BooleanField acceptConstKeyword = new BooleanField(false);
  public StringOptionsField languageIn = new StringOptionsField("ECMASCRIPT3", "ECMASCRIPT5", "ECMASCRIPT5_STRICT");
  public CheckLevelsField checkLevels = new CheckLevelsField();
  
  // Compilation (output)
  
  public StringOptionsField compilationLevel = new StringOptionsField("WHITESPACE_ONLY", "SIMPLE_OPTIMIZATIONS", "ADVANCED_OPTIMIZATIONS");
  public BooleanField formattingPrettyPrint = new BooleanField(false);
  public BooleanField formattingPrintInputDelimiter = new BooleanField(false);
  

  private static ClosureProjectPropertyRecord instance;
  public static ClosureProjectPropertyRecord getInstance() {
    if (instance == null) instance = new ClosureProjectPropertyRecord();
    return instance;
  }
  
}
