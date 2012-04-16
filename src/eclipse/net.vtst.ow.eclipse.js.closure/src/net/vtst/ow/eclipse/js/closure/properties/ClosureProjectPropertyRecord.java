package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.eclipse.easy.ui.properties.Record;
import net.vtst.eclipse.easy.ui.properties.fields.BooleanField;
import net.vtst.eclipse.easy.ui.properties.fields.EnumOptionsField;
import net.vtst.eclipse.easy.ui.properties.fields.FileField;
import net.vtst.eclipse.easy.ui.properties.fields.FileListField;

import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.jscomp.WarningLevel;

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
  
  public EnumOptionsField<WarningLevel> warningLevel = new EnumOptionsField<WarningLevel>(WarningLevel.class, WarningLevel.DEFAULT);
  public BooleanField thirdParty = new BooleanField(false);
  public BooleanField processClosurePrimitives = new BooleanField(true);
  public BooleanField processJQueryPrimitives = new BooleanField(false);
  public BooleanField acceptConstKeyword = new BooleanField(false);
  public EnumOptionsField<LanguageMode> languageIn = new EnumOptionsField<LanguageMode>(CompilerOptions.LanguageMode.class, LanguageMode.ECMASCRIPT3);
  public CheckLevelsField checkLevels = new CheckLevelsField();  

  private static ClosureProjectPropertyRecord instance;
  public static ClosureProjectPropertyRecord getInstance() {
    if (instance == null) instance = new ClosureProjectPropertyRecord();
    return instance;
  }
}
