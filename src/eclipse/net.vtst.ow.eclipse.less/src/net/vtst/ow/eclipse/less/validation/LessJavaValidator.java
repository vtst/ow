// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.vtst.eclipse.easyxtext.validation.config.AdditionalBooleanOption;
import net.vtst.eclipse.easyxtext.validation.config.ConfigurableCheck;
import net.vtst.eclipse.easyxtext.validation.config.ConfigurableDeclarativeValidator;
import net.vtst.ow.eclipse.less.LessMessages;
import net.vtst.ow.eclipse.less.less.Block;
import net.vtst.ow.eclipse.less.less.Declaration;
import net.vtst.ow.eclipse.less.less.HashColorTerm;
import net.vtst.ow.eclipse.less.less.IdentTerm;
import net.vtst.ow.eclipse.less.less.ImportStatement;
import net.vtst.ow.eclipse.less.less.IncompleteToplevelStatement;
import net.vtst.ow.eclipse.less.less.InnerRuleSet;
import net.vtst.ow.eclipse.less.less.InnerSelector;
import net.vtst.ow.eclipse.less.less.InnerStatement;
import net.vtst.ow.eclipse.less.less.KeyframesContents;
import net.vtst.ow.eclipse.less.less.KeyframesStatement;
import net.vtst.ow.eclipse.less.less.LessPackage;
import net.vtst.ow.eclipse.less.less.Mixin;
import net.vtst.ow.eclipse.less.less.MixinDefinitionGuards;
import net.vtst.ow.eclipse.less.less.MixinParameter;
import net.vtst.ow.eclipse.less.less.MixinParameters;
import net.vtst.ow.eclipse.less.less.MixinSelectors;
import net.vtst.ow.eclipse.less.less.MixinUtils;
import net.vtst.ow.eclipse.less.less.MixinVarParameter;
import net.vtst.ow.eclipse.less.less.NumberWithUnitTerm;
import net.vtst.ow.eclipse.less.less.NumericLiteral;
import net.vtst.ow.eclipse.less.less.Property;
import net.vtst.ow.eclipse.less.less.PseudoClassNthSpecialCase;
import net.vtst.ow.eclipse.less.less.RawProperty;
import net.vtst.ow.eclipse.less.less.StringTerm;
import net.vtst.ow.eclipse.less.less.StyleSheet;
import net.vtst.ow.eclipse.less.less.Term;
import net.vtst.ow.eclipse.less.less.TerminatedMixin;
import net.vtst.ow.eclipse.less.less.ToplevelRuleSet;
import net.vtst.ow.eclipse.less.less.ToplevelSelector;
import net.vtst.ow.eclipse.less.less.VariableDefinition;
import net.vtst.ow.eclipse.less.linking.LessMixinLinkingService;
import net.vtst.ow.eclipse.less.linking.LessMixinLinkingService.ICheckMixinError;
import net.vtst.ow.eclipse.less.linking.LessMixinLinkingService.MixinLink;
import net.vtst.ow.eclipse.less.scoping.LessImportStatementResolver;
import net.vtst.ow.eclipse.less.scoping.LessImportStatementResolver.ResolvedImportStatement;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.Check;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

public class LessJavaValidator extends AbstractLessJavaValidator {
}
