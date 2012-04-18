package net.vtst.ow.eclipse.js.closure.launching.linter;

import net.vtst.eclipse.easy.ui.properties.Record;
import net.vtst.eclipse.easy.ui.properties.fields.BooleanField;
import net.vtst.eclipse.easy.ui.properties.fields.ResourceListField;
import net.vtst.eclipse.easy.ui.properties.fields.StringField;
import net.vtst.ow.eclipse.js.closure.launching.compiler.ClosureCompilerLaunchConfigurationRecord;
import net.vtst.ow.eclipse.js.closure.properties.ClosureProjectPropertyRecord;

import org.eclipse.core.resources.IResource;

public class ClosureLinterLaunchConfigurationRecord extends Record {
  
  public ClosureLinterLaunchConfigurationRecord() {
    super.initializeByReflection();
  }
  
  // Inputs and output

  public ResourceListField<IResource> inputResources = 
      new ResourceListField<IResource>(IResource.class, ClosureCompilerLaunchConfigurationRecord.getJavaScriptResourceFilter());
  public StringField gjslintCommand = new StringField("gjslint");
  public StringField fixjsstyleCommand = new StringField("fixjsstyle");
  public BooleanField fixLintErrors = new BooleanField(false);
  public BooleanField useAsDefault = new BooleanField(true);

  // Project properties
  
  public BooleanField useProjectPropertiesForLinterChecks = new BooleanField(true);
  public ClosureProjectPropertyRecord.LinterChecksRecord checks = new ClosureProjectPropertyRecord.LinterChecksRecord(); 
  
  private static ClosureLinterLaunchConfigurationRecord instance;
  public static ClosureLinterLaunchConfigurationRecord getInstance() {
    if (instance == null) instance = new ClosureLinterLaunchConfigurationRecord();
    return instance;
  }

}
