package net.vtst.ow.eclipse.js.closure.properties;

import net.vtst.eclipse.easy.ui.properties.Record;
import net.vtst.eclipse.easy.ui.properties.fields.BooleanField;
import net.vtst.eclipse.easy.ui.properties.fields.EnumOptionsField;
import net.vtst.eclipse.easy.ui.properties.fields.FileField;
import net.vtst.eclipse.easy.ui.properties.fields.FileListField;
import net.vtst.eclipse.easy.ui.properties.fields.FlagListField;
import net.vtst.eclipse.easy.ui.properties.fields.StringField;

import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.jscomp.WarningLevel;

public class ClosureProjectPropertyRecord extends Record {
  
  public ClosureProjectPropertyRecord() {
    super.initializeByReflection();
  }

  // Includes
  
  public static class IncludesRecord extends Record {
    public BooleanField useDefaultClosureBasePath = new BooleanField(true);
    public FileField closureBasePath = new FileField(null, FileField.Type.DIRECTORY);
    public FileListField otherLibraries = new FileListField(FileListField.Type.DIRECTORY);
    public FileListField externs = new FileListField(FileListField.Type.FILE, 
        new String[]{"*.js", "*"}, new String[]{"js", "all"});
    public BooleanField useOnlyCustomExterns = new BooleanField(false);    
  }
  
  public IncludesRecord includes = new IncludesRecord();
    
  // Compilation (checks)
  
  public static class ChecksRecord extends Record {
    public EnumOptionsField<WarningLevel> warningLevel = new EnumOptionsField<WarningLevel>(WarningLevel.class, WarningLevel.DEFAULT);
    public BooleanField thirdParty = new BooleanField(false);
    public BooleanField processClosurePrimitives = new BooleanField(true);
    public BooleanField processJQueryPrimitives = new BooleanField(false);
    public BooleanField acceptConstKeyword = new BooleanField(false);
    public EnumOptionsField<LanguageMode> languageIn = new EnumOptionsField<LanguageMode>(CompilerOptions.LanguageMode.class, LanguageMode.ECMASCRIPT3);
    public CheckLevelsField checkLevels = new CheckLevelsField();  
  }
  
  public ChecksRecord checks = new ChecksRecord();
  
  // Linter
  
  /*
   * TODO
   *     - blank_lines_at_top_level: validatesnumber of blank lines between blocks at
    top level.
    - indentation: checks correct indentation of code.
    - well_formed_author: validates the @author JsDoc tags.
    - no_braces_around_inherit_doc: forbids braces around @inheritdoc JsDoc
    tags.
    - braces_around_type: enforces braces around types in JsDoc tags.
    - optional_type_marker: checks correct use of optional marker = in param
    types.

   */
  
  public static class LinterRecord extends Record {
    public StringField customJsdocTags = new StringField("");
    public FlagListField lintErrorChecks = new FlagListField(new String[]{
        "blank_lines_at_top_level", "indentation", "well_formed_author", "no_braces_around_inherit_doc", "braces_around_type", "optional_type_marker"});
    public BooleanField strictClosureStyle = new BooleanField(false);
    public BooleanField missingJsdoc = new BooleanField(true);
    public StringField ignoreLintErrors = new StringField("");
  }
  
  public LinterRecord linter = new LinterRecord();

  private static ClosureProjectPropertyRecord instance;
  public static ClosureProjectPropertyRecord getInstance() {
    if (instance == null) instance = new ClosureProjectPropertyRecord();
    return instance;
  }
}
