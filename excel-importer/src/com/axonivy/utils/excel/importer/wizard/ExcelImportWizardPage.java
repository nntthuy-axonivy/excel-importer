package com.axonivy.utils.excel.importer.wizard;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;

import ch.ivyteam.ivy.designer.ui.wizard.restricted.WizardStatus;
import ch.ivyteam.swt.dialogs.SwtCommonDialogs;

public class ExcelImportWizardPage extends WizardPage implements IWizardPage {

  static final String PAGE_ID = "ImportExcel";
  private final ExcelImportProcessor processor;

  private ExcelUi ui;
  private UiState state;

  public ExcelImportWizardPage(ExcelImportProcessor processor) {
    super(PAGE_ID);
    setMessage(processor.getWizardPageOkMessage(PAGE_ID));
    this.processor = processor;
    setPageComplete(false);
    this.state = new UiState(processor, ()->getImportHistory());
  }

  @Override
  public void createControl(Composite parent) {
    this.ui = new ExcelUi(parent);

    ui.fileBrowser.addListener(SWT.Selection, evt -> handleDestinationBrowseButtonPressed());

    setButtonLayoutData(ui.fileBrowser);
    setControl(ui);

    var history = getImportHistory();
    if (!history.isEmpty()) {
      fileSelected(history.get(0));
    }

   state.bind(ui);
   state.watch(newVal -> handleInputChanged());
  }

  private List<String> getImportHistory() {
    IDialogSettings dialogSettings = getDialogSettings();
    if (dialogSettings == null) {
      return List.of();
    }
    String[] destinations = dialogSettings.getArray(ExcelImportUtil.DESTINATION_KEY);
    if (destinations == null) {
      return List.of();
    }
    return Stream.of(destinations)
      .filter(file -> file.endsWith(ExcelImportUtil.DEFAULT_EXTENSION))
      .toList();
  }

  boolean finish() {
    if (processor.wizardFinishInvoked() && executeImport()) {
      saveDialogSettings();
      return true;
    }
    return false;
  }

  protected void handleInputChanged() {
    var status = WizardStatus.createOkStatus();
    status.merge(processor.validateImportFileExits());
    status.merge(processor.validateEntity());
    status.merge(processor.validateProject());
    status.merge(processor.validatePersistence());
    setPageComplete(status.isLowerThan(WizardStatus.ERROR));
    if (status.isOk()) {
      setMessage(processor.getWizardPageOkMessage(PAGE_ID), 0);
    } else if (status.isFatal()) {
      SwtCommonDialogs.openBugDialog(getControl(), status.getFatalError());
    } else {
      setMessage(status.getMessage(), status.getSeverity());
    }
  }

  private void saveDialogSettings() {
    List<String> destinations = new LinkedList<>(getImportHistory());
    String path = state.file.getSelection();
    String lowerCasePath = path.toLowerCase();
    if (destinations.contains(path)) {
      destinations.remove(path);
      destinations.add(0, path);
      getDialogSettings().put(ExcelImportUtil.DESTINATION_KEY, destinations.toArray(String[]::new));
    } else if (lowerCasePath.endsWith(ExcelImportUtil.DEFAULT_EXTENSION)) {
      if (destinations.size() == 10) {
        destinations.remove(destinations.size() - 1);
      }
      destinations.add(0, path);
      getDialogSettings().put(ExcelImportUtil.DESTINATION_KEY, destinations.toArray(String[]::new));
    }
  }

  private void handleDestinationBrowseButtonPressed() {
    FileDialog dialog = new FileDialog(getContainer().getShell(), 0);
    dialog.setFilterExtensions(ExcelImportUtil.IMPORT_TYPE);
    dialog.setText("Select import file");
    dialog.setFilterPath(StringUtils.EMPTY);
    String currentSourceString = state.file.getSelection();
    dialog.setFilterPath(currentSourceString);
    String selectedFileName = dialog.open();
    if (selectedFileName != null) {
      fileSelected(selectedFileName);
    }
  }

  private void fileSelected(String selectedFileName) {
    state.file.setSelection(selectedFileName);
    state.entity.setText(proposeName(selectedFileName));
  }

  private static String proposeName(String selection) {
    if (selection == null) {
      return "";
    }
    String fileName = new File(selection).getName();
    String entityName = StringUtils.substringBeforeLast(fileName, ".");
    entityName = StringUtils.capitalize(entityName);
    return entityName;
  }

  private boolean executeImport() {
    Combo dst = ui.importFile.getCombo();
    if (dst.getText().lastIndexOf(File.separator) == -1) {
      dst.setText(ExcelImportUtil.DEFAULT_FILTER_PATH + File.separator + dst.getText());
      processor.setImportFile(dst.getText());
    }
    try {
      getContainer().run(true, true, processor);
    } catch (InterruptedException localInterruptedException) {
      return false;
    } catch (InvocationTargetException e) {
      SwtCommonDialogs.openBugDialog(getControl(), e.getTargetException());
      return false;
    }
    var status = processor.getStatus();
    if (status.isOK()) {
      SwtCommonDialogs.openInformationDialog(getShell(), "Express Import", "Successfully imported");
    } else {
      SwtCommonDialogs.openErrorDialog(getContainer().getShell(),
              "Problems during import of Excel as Dialog", status.getMessage(), status.getException());
      return false;
    }
    return true;
  }
}
