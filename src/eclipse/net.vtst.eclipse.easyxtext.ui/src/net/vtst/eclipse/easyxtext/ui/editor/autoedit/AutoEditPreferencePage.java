package net.vtst.eclipse.easyxtext.ui.editor.autoedit;

import java.lang.reflect.Method;

import net.vtst.eclipse.easyxtext.util.IEasyMessages;

import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.xtext.ui.editor.autoedit.AbstractEditStrategyProvider;

import com.google.inject.Inject;

@SuppressWarnings("restriction")
public class AutoEditPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  @Inject
  AbstractEditStrategyProvider abstractEditStrategyProvider;
  
  EasyEditStrategyProvider editStrategyProvider;
  
  // **************************************************************************
  // User interface
      
  private Table list;

  public Control createContents(Composite parent) {
    assert abstractEditStrategyProvider instanceof EasyEditStrategyProvider;
    editStrategyProvider = (EasyEditStrategyProvider) abstractEditStrategyProvider;
    Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
    list = new Table(composite, SWT.V_SCROLL | SWT.CHECK | SWT.BORDER);
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.horizontalSpan = 1;
    list.setLayoutData(gd);
    TableColumn column1 = new TableColumn(list, SWT.NONE);
    fillList();
    column1.pack();
    SWTFactory.createLabel(parent, messages.getString("AutoEditPreferencePage_message"), 1);
    return composite;
  }

  private void fillList() {
    for (Method method: editStrategyProvider.getConfigureMethods()) {
      TableItem item = new TableItem(list, SWT.NONE);
      item.setText(new String[] {getMethodLabel(method)});
      item.setData(method);
      item.setChecked(editStrategyProvider.getMethodPreferenceValue(method));
    }
  }

  @Inject(optional=true)
  private IEasyMessages messages;
  
  private String getMethodLabel(Method method) {
    String key = editStrategyProvider.getMessageKey(method);
    if (messages == null) return key;
    else return messages.getString(key);
  }
  
  @Override
  protected void performDefaults() {
    for (TableItem item: list.getItems()) {
      editStrategyProvider.resetPreferenceValue((Method) item.getData());
      item.setChecked(editStrategyProvider.getMethodPreferenceValue((Method) item.getData()));
    }
    super.performDefaults();
  }
  
  @Override
  public boolean performOk() {
    for (TableItem item: list.getItems()) {
      editStrategyProvider.setMethodPreferenceValue((Method) item.getData(), item.getChecked());
    }
    return super.performOk();
  }

  @SuppressWarnings("deprecation")
  public void init(IWorkbench workbench) {
    this.setPreferenceStore(workbench.getPreferenceStore());
  }
  
}
