package net.vtst.ow.eclipse.js.closure.launching.compiler;

import java.util.regex.Pattern;

import net.vtst.eclipse.easy.ui.properties.Record;
import net.vtst.eclipse.easy.ui.properties.fields.BooleanField;
import net.vtst.eclipse.easy.ui.properties.fields.EnumOptionsField;
import net.vtst.eclipse.easy.ui.properties.fields.ResourceListField;
import net.vtst.eclipse.easy.ui.properties.fields.ResourceListField.FileType;
import net.vtst.eclipse.easy.ui.properties.fields.ResourceListField.Or;
import net.vtst.eclipse.easy.ui.properties.fields.ResourceListField.ProjectNature;
import net.vtst.eclipse.easy.ui.properties.fields.ResourceListField.ResourceType;
import net.vtst.eclipse.easy.ui.properties.fields.StringField;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.builder.ClosureNature;
import net.vtst.ow.eclipse.js.closure.properties.project.ClosureProjectPropertyRecord;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;

import com.google.javascript.jscomp.CompilationLevel;

public class ClosureCompilerLaunchConfigurationRecord extends Record {
  
  public ClosureCompilerLaunchConfigurationRecord() {
    super.initializeByReflection();
  }
  
  // Inputs and output
  
  public static ResourceListField.IFilter<IResource> getJavaScriptResourceFilter() {
    return new Or<IResource>(
        new ProjectNature<IResource>(ClosureNature.NATURE_ID),
        new ResourceType<IResource>(IFolder.class),
        new FileType<IResource>(Pattern.compile(".*\\.js"), Platform.getContentTypeManager().getContentType(OwJsClosurePlugin.JS_CONTENT_TYPE_ID)));
  }

  public ResourceListField<IResource> inputResources = 
      new ResourceListField<IResource>(IResource.class, getJavaScriptResourceFilter());
  public BooleanField manageClosureDependencies = new BooleanField(true);
  public StringField outputFile = new StringField("");
  public BooleanField useDefaultOutputFile = new BooleanField(true);

  // Compilation options

  public EnumOptionsField<CompilationLevel> compilationLevel = new EnumOptionsField<CompilationLevel>(CompilationLevel.class);
  public BooleanField formattingPrettyPrint = new BooleanField(false);
  public BooleanField formattingPrintInputDelimiter = new BooleanField(false);
  public BooleanField generateExports = new BooleanField(false);
  public StringField closureEntryPoints = new StringField("");

  // Project properties
  
  public BooleanField useProjectPropertiesForIncludes = new BooleanField(true);
  public ClosureProjectPropertyRecord.IncludesRecord includes = new ClosureProjectPropertyRecord.IncludesRecord(); 
  public BooleanField useProjectPropertiesForChecks = new BooleanField(true);
  public ClosureProjectPropertyRecord.ChecksRecord checks = new ClosureProjectPropertyRecord.ChecksRecord(); 
  
  private static ClosureCompilerLaunchConfigurationRecord instance;
  public static ClosureCompilerLaunchConfigurationRecord getInstance() {
    if (instance == null) instance = new ClosureCompilerLaunchConfigurationRecord();
    return instance;
  }

}
