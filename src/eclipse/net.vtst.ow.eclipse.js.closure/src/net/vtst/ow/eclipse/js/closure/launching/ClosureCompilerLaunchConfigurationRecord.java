package net.vtst.ow.eclipse.js.closure.launching;

import java.util.regex.Pattern;

import net.vtst.eclipse.easy.ui.properties.Record;
import net.vtst.eclipse.easy.ui.properties.fields.BooleanField;
import net.vtst.eclipse.easy.ui.properties.fields.ResourceListField;
import net.vtst.eclipse.easy.ui.properties.fields.StringField;
import net.vtst.eclipse.easy.ui.properties.fields.ResourceListField.FileType;
import net.vtst.eclipse.easy.ui.properties.fields.ResourceListField.Or;
import net.vtst.eclipse.easy.ui.properties.fields.ResourceListField.ProjectNature;
import net.vtst.eclipse.easy.ui.properties.fields.ResourceListField.ResourceType;
import net.vtst.eclipse.easy.ui.properties.fields.StringOptionsField;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.builder.ClosureNature;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;

public class ClosureCompilerLaunchConfigurationRecord extends Record {
  
  public ClosureCompilerLaunchConfigurationRecord() {
    super.initializeByReflection();
  }
  
  public StringOptionsField compilationLevel = new StringOptionsField("WHITESPACE_ONLY", "SIMPLE_OPTIMIZATIONS", "ADVANCED_OPTIMIZATIONS");
  public BooleanField formattingPrettyPrint = new BooleanField(false);
  public BooleanField formattingPrintInputDelimiter = new BooleanField(false);
  public BooleanField generateExports = new BooleanField(false);
  public ResourceListField<IResource> inputResources = 
      new ResourceListField<IResource>(IResource.class, new Or<IResource>(
          new ProjectNature<IResource>(ClosureNature.NATURE_ID),
          new ResourceType<IResource>(IFolder.class),
          new FileType<IResource>(Pattern.compile(".*\\.js"), Platform.getContentTypeManager().getContentType(OwJsClosurePlugin.JS_CONTENT_TYPE_ID))
          ));
  public StringField closureEntryPoints = new StringField("");

  private static ClosureCompilerLaunchConfigurationRecord instance;
  public static ClosureCompilerLaunchConfigurationRecord getInstance() {
    if (instance == null) instance = new ClosureCompilerLaunchConfigurationRecord();
    return instance;
  }

}
