package net.vtst.ow.eclipse.less.scoping;

import java.util.Collections;
import java.util.List;

import net.vtst.ow.eclipse.less.less.HashOrClassRef;
import net.vtst.ow.eclipse.less.less.HashOrClassRefTarget;
import net.vtst.ow.eclipse.less.less.LessUtils;
import net.vtst.ow.eclipse.less.less.Mixin;
import net.vtst.ow.eclipse.less.less.MixinUtils;
import net.vtst.ow.eclipse.less.less.TerminatedMixin;
import net.vtst.ow.eclipse.less.less.MixinUtils.Prototype;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Tuples;
import org.eclipse.xtext.util.internal.Stopwatches;
import org.eclipse.xtext.util.internal.Stopwatches.StoppedTask;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LessLinkingService extends DefaultLinkingService {
  
  private static final Logger logger = Logger.getLogger(DefaultLinkingService.class);

  @Inject
  private IQualifiedNameConverter qualifiedNameConverter;
  
  // The cache contains pairs (MixinUtils.Prototype.class, HashOrClassRefTarget).
  @Inject
  private IResourceScopeCache cache;
  
  public Prototype getPrototypeForMixinDefinition(final HashOrClassRefTarget hashOrClass) {
    return cache.get(Tuples.pair(Prototype.class, hashOrClass), hashOrClass.eResource(), new Provider<Prototype>() {
      public Prototype get() {
        EObject mixinCall = LessUtils.getNthAncestor(hashOrClass, 2);
        if ((mixinCall instanceof TerminatedMixin)) {
          return new Prototype((TerminatedMixin) mixinCall);
        } else {
          return new Prototype(null);
        }
      }
    });
  }

  protected IEObjectDescription getBestMatchForHashOrClassRef(HashOrClassRef context, Iterable<IEObjectDescription> eObjectDescriptions) {
    EObject mixinCall = LessUtils.getNthAncestor(context, 2);
    if (mixinCall instanceof Mixin) {
      MixinUtils.Helper mixinHelper = MixinUtils.newHelper((Mixin) mixinCall);
      for (IEObjectDescription eObjectDescription : eObjectDescriptions) {
        EObject eObject = eObjectDescription.getEObjectOrProxy();
        if (eObject instanceof HashOrClassRefTarget) {
          MixinUtils.Prototype prototype = getPrototypeForMixinDefinition((HashOrClassRefTarget) eObject);
          if (prototype.checkMixinCall(mixinHelper, null))
            return eObjectDescription;
        }
      }
    }
    return getBestMatchDefault(context, eObjectDescriptions);
  }

  protected IEObjectDescription getBestMatchDefault(EObject context, Iterable<IEObjectDescription> eObjectDescriptions) {
    for (IEObjectDescription eObjectDescription : eObjectDescriptions) {
      return eObjectDescription;
    }
    return null;
  }

  protected IEObjectDescription getBestMatch(EObject context, Iterable<IEObjectDescription> eObjectDescriptions) {
    if (context instanceof HashOrClassRef) {
      return getBestMatchForHashOrClassRef((HashOrClassRef) context, eObjectDescriptions);
    } else {
      return getBestMatchDefault(context, eObjectDescriptions);
    }
  }

  public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node)
      throws IllegalNodeException {
    final EClass requiredType = ref.getEReferenceType();
    if (requiredType == null)
      return Collections.<EObject> emptyList();

    final String crossRefString = getCrossRefNodeAsString(node);
    if (crossRefString != null && !crossRefString.equals("")) {
      if (logger.isDebugEnabled()) {
        logger.debug("before getLinkedObjects: node: '" + crossRefString + "'");
      }
      StoppedTask task = Stopwatches.forTask("Crosslink resolution (DefaultLinkingService.getLinkedObjects)");
      try {
        task.start();
        final IScope scope = getScope(context, ref);
        QualifiedName qualifiedLinkName =  qualifiedNameConverter.toQualifiedName(crossRefString);
        IEObjectDescription eObjectDescription = getBestMatch(context, scope.getElements(qualifiedLinkName));
        if (logger.isDebugEnabled()) {
          logger.debug("after getLinkedObjects: node: '" + crossRefString + "' result: " + eObjectDescription);
        }
        if (eObjectDescription != null) 
          return Collections.singletonList(eObjectDescription.getEObjectOrProxy());
      } finally {
        task.stop();
      }
    }
    return Collections.emptyList();
  }

}
