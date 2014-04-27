package net.vtst.ow.eclipse.less.ui.contentassist;

import java.util.ArrayList;
import java.util.List;

import net.vtst.ow.eclipse.less.less.Block;
import net.vtst.ow.eclipse.less.less.Combinator;
import net.vtst.ow.eclipse.less.less.HashOrClass;
import net.vtst.ow.eclipse.less.less.InnerSelector;
import net.vtst.ow.eclipse.less.less.MixinUtils;
import net.vtst.ow.eclipse.less.less.SimpleSelector;
import net.vtst.ow.eclipse.less.less.SimpleSelectorWithoutRoot;
import net.vtst.ow.eclipse.less.less.StyleSheet;
import net.vtst.ow.eclipse.less.less.ToplevelSelector;
import net.vtst.ow.eclipse.less.less.VariableSelector;
import net.vtst.ow.eclipse.less.scoping.MixinContext;
import net.vtst.ow.eclipse.less.scoping.MixinPath;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

public class MixinContentAssistContext {

  public static IMixinContentAssistContext create(EObject model) {
    if (model instanceof Block || model instanceof StyleSheet) {
      // Case 1: Block (always or almost always a first selector)
      //   #^ #f^ (top level or inner)
      //   #foo #f^ (top level)
      return new Empty();
    } else if (model instanceof Combinator) {
      // Case 2: ToplevelSelector > DescendantSelector
      //   #foo #f^ (top level)
      //   #foo #f^ .bar {} (top level or inner)
      // Case 3: InnerSelector > DescendantSelector
      //   #foo #f^ (inner)
      return new FromCombinator((Combinator) model);
    } else if (model instanceof HashOrClass) {
      // Case 4: Hash > SimpleSelectorWithoutRoot > ToplevelSelector
      //   #f^ .bar {} (top level or inner)
      return new FromHashOrClass((HashOrClass) model);
    }
    // Default case
    return new FromMixinContext(new MixinContext(model));
  }

  // **************************************************************************
  // Case 1

  public static class Empty implements IMixinContentAssistContext {
    
    public boolean isValid() { return true; }

    public MixinPath getPath() { return new MixinPath(""); }

    public int getIndex() { return 0; }
    
  }

  // **************************************************************************
  // Cases 2 and 3
    
  public static abstract class AbstractMixinContentAssistContext implements IMixinContentAssistContext {
    
    protected boolean isValid;
    protected List<String> path = new ArrayList<String>();

    protected boolean addPathFragmentsFromSimpleSelector(SimpleSelector selector, HashOrClass stopAfter) {
      if (selector instanceof SimpleSelectorWithoutRoot) {
        SimpleSelectorWithoutRoot selector2 = (SimpleSelectorWithoutRoot) selector;
        for (EObject criteria : selector2.getCriteria()) {
          if (criteria instanceof HashOrClass) {
            path.add(((HashOrClass) criteria).getIdent());
            if (criteria.equals(stopAfter)) return true;
          } else if (criteria instanceof VariableSelector) {
            // ignore
          } else {
            return false;
          }
        }
        return true;
      } else {
        return false;
      }
    }
    
    public boolean isValid() { return isValid; }
    
    public MixinPath getPath() { return new MixinPath(this.path); }
    
    public int getIndex() { return this.path.size() - 1; }
    
  }

  public static class FromCombinator extends AbstractMixinContentAssistContext {

    public FromCombinator(Combinator combinator) {
      this.isValid = init(combinator);
    }
    
    private boolean init(Combinator combinator) {
      EObject container = combinator.eContainer();
      if (container instanceof ToplevelSelector) {
        ToplevelSelector toplevelSelector = (ToplevelSelector) container;
        return init(combinator, toplevelSelector.getSelector(), toplevelSelector.getCombinator(), false);
      } else if (container instanceof InnerSelector) {
        InnerSelector innerSelector = (InnerSelector) container;
        return init(combinator, innerSelector.getSelector(), innerSelector.getCombinator(), false);
      } else {
        return false;
      }
    }
    
    private boolean init(
        Combinator combinator,
        EList<SimpleSelector> selectors, EList<Combinator> combinators, boolean hasRootCombinator) {
      int combinatorOffset = hasRootCombinator ? 1 : 0;
      for (int i = 0; i < selectors.size(); ++i) {
        if (!addPathFragmentsFromSimpleSelector(selectors.get(i), null))
          return false;
        if (combinator.equals(combinators.get(i + combinatorOffset)))
          break;
      }
      path.add("");
      return true;
    }
    
  }
  
  // **************************************************************************
  // Case 4

  public static class FromHashOrClass extends AbstractMixinContentAssistContext {

    public FromHashOrClass(HashOrClass hashOrClass) {
      this.isValid = init(hashOrClass);
    }
    
    private boolean init(HashOrClass hashOrClass) {
      EObject container = hashOrClass.eContainer();
      if (!(container instanceof SimpleSelector)) return false;
      return addPathFragmentsFromSimpleSelector((SimpleSelector) container, hashOrClass);
    }
    
  }
  
  // **************************************************************************
  // Default case
  
  public static class FromMixinContext implements IMixinContentAssistContext {
    
    private MixinContext mixinContext;

    public FromMixinContext(MixinContext mixinContext) {
      this.mixinContext = mixinContext;
    }

    public boolean isValid() { return mixinContext.isValid(); }

    public MixinPath getPath() { return MixinUtils.getPath(mixinContext.getMixin()); }

    public int getIndex() { return mixinContext.getSelectorIndex(); }
  }
  
}
