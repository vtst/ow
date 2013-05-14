// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less;

import net.vtst.eclipse.easyxtext.guice.EasyXtextModule;
import net.vtst.eclipse.easyxtext.nature.IEasyProjectNature;
import net.vtst.ow.eclipse.less.formatting.LessHiddenTokenHelper;
import net.vtst.ow.eclipse.less.less.LessPackage;
import net.vtst.ow.eclipse.less.linking.LessLinkingService;
import net.vtst.ow.eclipse.less.linking.LessMixinLinkingService;
import net.vtst.ow.eclipse.less.nature.LessProjectNature;
import net.vtst.ow.eclipse.less.parser.LessValueConverterService;
import net.vtst.ow.eclipse.less.resource.LessLocationInFileProvider;
import net.vtst.ow.eclipse.less.resource.LessResourceDescriptionStrategy;
import net.vtst.ow.eclipse.less.scoping.LessMixinScopeProvider;
import net.vtst.ow.eclipse.less.scoping.LessQualifiedNameProvider;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.conversion.IValueConverterService;
import org.eclipse.xtext.linking.ILinkingDiagnosticMessageProvider;
import org.eclipse.xtext.linking.ILinkingService;
import org.eclipse.xtext.parsetree.reconstr.IHiddenTokenHelper;
import org.eclipse.xtext.resource.IDefaultResourceDescriptionStrategy;
import org.eclipse.xtext.resource.ILocationInFileProvider;

import com.google.inject.Binder;

/**
 * Use this class to register components to be used at runtime / without the Equinox extension registry.
 */
public class LessRuntimeModule extends net.vtst.ow.eclipse.less.AbstractLessRuntimeModule {
  public static String PLUGIN_ID = "net.vtst.ow.eclipse.less";
  public static String CONTENT_TYPE_ID = "net.vtst.ow.eclipse.less";
  
  public static String LESS_EXTENSION = "less";
  public static String CSS_EXTENSION = "css";

  public LessRuntimeModule() {
    CssProfile.initializeRegistry();
  }
    
  public void configure(Binder binder) {
    super.configure(binder);
    binder.install(new EasyXtextModule());
    //binder.bind(LessHiddenTokenHelper.class).to(IHiddenTokenHelper.class);
  }
  
  public EPackage bindEPackage() {
    return LessPackage.eINSTANCE;
  }
  
  @Override
  public Class<? extends IValueConverterService> bindIValueConverterService() {
    return LessValueConverterService.class;
  }
  
  public Class<? extends org.eclipse.xtext.naming.IQualifiedNameProvider> bindIQualifiedNameProvider() {
    return LessQualifiedNameProvider.class;
  }
  
  public Class<? extends ILinkingDiagnosticMessageProvider.Extended> bindILinkingDiagnosticMessageProvider() {
    return net.vtst.ow.eclipse.less.scoping.LessLinkingDiagnosticMessageProvider.class;
  }
  
  public Class<? extends ILocationInFileProvider> bindILocationInFileProvider() {
    return LessLocationInFileProvider.class;
  }

  public Class<? extends IHiddenTokenHelper> bindIHiddenTokenHelper() {
    return LessHiddenTokenHelper.class;
  }
  
  public Class<? extends IDefaultResourceDescriptionStrategy> bindIDefaultResourceDescriptionStrategy() {
    return LessResourceDescriptionStrategy.class;
  }
  
  public Class<? extends ILinkingService> bindILinkingService() {
    return LessLinkingService.class;
  }

  public Class<? extends LessMixinLinkingService> bindLessMixinLinkingService() {
    return LessMixinLinkingService.class;
  }

  public Class<? extends LessMixinScopeProvider> bindLessMixinScopeProvider() {
    return LessMixinScopeProvider.class;
  }


  public Class<? extends IEasyProjectNature> bindIEasyProjectNature() {
    return LessProjectNature.class;
  }
}
