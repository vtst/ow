// EasyXtext
// (c) Vincent Simonet, 2011

package net.vtst.eclipse.easyxtext.ui.syntaxcoloring;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.vtst.eclipse.easyxtext.guice.PostInject;
import net.vtst.eclipse.easyxtext.util.IEasyMessages;

import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfiguration;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfigurationAcceptor;

import com.google.inject.Inject;

/**
 * Abstract class to implement an highlighting configuration. The highlighting
 * style are automatically created at initialization by considering the field of
 * the class whose type is {@code EasyTextStyle} (or a subclass of it), see
 * {@code initializeByReflection} and {@code configure}. The display name of
 * each style is automatically extracted from a message bundle, see
 * {@code getDisplayNameForHighlightingConfiguration}.
 * 
 * @author Vincent Simonet
 */
public abstract class EasyHighlightingConfiguration implements
    IHighlightingConfiguration {

  @Inject
  private IEasyMessages messages;

  /**
   * Get the bundle to be used for getting display names. The default
   * implementation returns the class bound by dependency injection. Clients may
   * override this.
   * 
   * @return the message bundle.
   */
  protected IEasyMessages getMessageBundle() {
    return messages;
  }

  /**
   * Get the display name for an highlighting configuration. The default
   * implementation returns the message whose ID is "highlighting__<id>".
   * 
   * @param id
   *          The id of the highlighting configuration.
   * @return The display name of the highlighting configuration.
   */
  protected String getDisplayNameForHighlightingConfiguration(String id) {
    return getMessageBundle().getString("highlighting__" + id);
  }

  /**
   * The list of attributes, which is extracted by reflection.
   */
  private List<EasyTextAttribute> attributes = new ArrayList<EasyTextAttribute>();

  /**
   * This method is automatically called after dependency injection. It
   * initializes the class by reflectively looking at the field members whose
   * type is {@code EasyTextAttribute} (or a super-class of it). Clients should
   * neither call this class nor override it.
   */
  @PostInject
  final public void initializeByReflection() {
    for (Field field : this.getClass().getFields()) {
      try {
        Object fieldValue = field.get(this);
        if (fieldValue instanceof EasyTextAttribute) {
          EasyTextAttribute textStyle = ((EasyTextAttribute) fieldValue);
          textStyle.setId(field.getName());
          attributes.add(textStyle);
        }
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * This method is called by the framework and allows clients to register the
   * default styles for the semantic highlighting stage. The default
   * implementation scans the class fields, and create a style for every field
   * whose value inherits from EasyTextStyle. The ID of every style is the field
   * name.
   * 
   * @param acceptor
   *          the acceptor is used to announce the various default styles. It is
   *          never <code>null</code>.
   * @see org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfiguration#configure(org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfigurationAcceptor)
   */
  @Override
  public void configure(IHighlightingConfigurationAcceptor acceptor) {
    for (EasyTextAttribute attribute : attributes) {
      String id = attribute.getId();
      acceptor.acceptDefaultHighlighting(id,
          getDisplayNameForHighlightingConfiguration(id), attribute);
    }
  }

}
