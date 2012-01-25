package net.vtst.ow.eclipse.soy.scoping;

import java.util.Collections;

import net.vtst.eclipse.easyxtext.scoping.EasyBuiltinGlobalScopeProvider;
import net.vtst.ow.eclipse.soy.SoyRuntimeModule;

import org.eclipse.xtext.scoping.IGlobalScopeProvider;

import com.google.inject.Inject;

public class SoyBuiltinGlobalScopeProvider extends EasyBuiltinGlobalScopeProvider {

  @Inject
  private IGlobalScopeProvider globalScopeProvider;

  @Override
  public String getBundleSymbolicName() {
    return SoyRuntimeModule.PLUGIN_ID;
  }

  @Override
  public Iterable<String> getBuiltinFiles() {
    return Collections.singleton("data/builtin.soy");
  }
  
  protected IGlobalScopeProvider getDelegatedGlobalScopeProvider() {
    return globalScopeProvider;
  }

  
}
