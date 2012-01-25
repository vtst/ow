// net.vtst.ow.eclipse.jsdt4goog
// (c) Vincent Simonet, 2011.

package net.vtst.ow.eclipse.jsdt4goog.infer;

import org.eclipse.wst.jsdt.core.infer.IInferEngine;
import org.eclipse.wst.jsdt.core.infer.IInferenceFile;
import org.eclipse.wst.jsdt.core.infer.InferrenceProvider;
import org.eclipse.wst.jsdt.core.infer.RefactoringSupport;
import org.eclipse.wst.jsdt.core.infer.ResolutionConfiguration;


/**
 * Inference provider that provides the extended inference engine {@link ExtInferEngine}.
 * @author Vincent Simonet
 */
public class ExtInferenceProvider implements InferrenceProvider {

  @Override
  public IInferEngine getInferEngine() {
    ExtInferEngine engine = new ExtInferEngine();
    engine.inferenceProvider = this;
    return engine;
  }

  @Override
  public int applysTo(IInferenceFile scriptFile) {
    // TODO This should be refined, to enable/disable this on a per-file basis.
    return InferrenceProvider.ONLY_THIS;
  }

  @Override
  public String getID() {
    return ExtInferenceProvider.class.getName();
  }

  @Override
  public ResolutionConfiguration getResolutionConfiguration() {
    ResolutionConfiguration config = new ResolutionConfiguration();
    return config;    
  }

  @Override
  public RefactoringSupport getRefactoringSupport() {
    return null;
  }

}
