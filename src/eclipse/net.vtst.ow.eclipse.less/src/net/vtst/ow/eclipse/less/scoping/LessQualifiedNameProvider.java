// net.vtst.ow.eclipse.less: An Eclipse module for LESS (http://lesscss.org)
// (c) Vincent Simonet, 2011.  All rights reserved.

package net.vtst.ow.eclipse.less.scoping;

import net.vtst.eclipse.easyxtext.scoping.EasyQualifiedNameProvider;
import net.vtst.ow.eclipse.less.less.VariableDefinitionIdent;

import org.eclipse.xtext.naming.QualifiedName;

public class LessQualifiedNameProvider extends EasyQualifiedNameProvider {

  public QualifiedName _getFullyQualifiedName(VariableDefinitionIdent obj) {
    // We return null, otherwise errors are reported for duplicated names
    return null;
  }

}
