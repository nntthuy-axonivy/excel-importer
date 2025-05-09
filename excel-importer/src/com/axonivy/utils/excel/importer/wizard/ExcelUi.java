package com.axonivy.utils.excel.importer.wizard;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ch.ivyteam.swt.shell.RootTestShell;
import ch.ivyteam.swt.widgets.ExtendableComboViewer;

class ExcelUi extends Composite {

  public final ComboViewer importFile;
  public final Button fileBrowser;
  public final Text entity;
  public final ComboViewer targetProject;
  public final ComboViewer persistence;

  public ExcelUi(Composite parent) {
    super(parent, SWT.NONE);

    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    setLayout(layout);
    setLayoutData(new GridData(272));

    Label destinationLabel = new Label(this, 0);
    destinationLabel.setText("From file");
    importFile = new ExtendableComboViewer(this, 2052);
    var dataDest = new GridData(768);
    dataDest.widthHint = 250;
    importFile.getCombo().setLayoutData(dataDest);
    fileBrowser = new Button(this, 8);
    fileBrowser.setText("Browse ...");

    Label entityLabel = new Label(this, SWT.NONE);
    entityLabel.setText("Entity");
    this.entity = new Text(this, SWT.BORDER);
    GridData entGrid = new GridData(768);
    entGrid.widthHint = 250;
    entGrid.horizontalSpan = 2;
    entity.setLayoutData(entGrid);

    Label sourceLabel = new Label(this, 0);
    sourceLabel.setText("Project");
    this.targetProject = new ComboViewer(this, SWT.BORDER);
    GridData data = new GridData(768);
    data.widthHint = 250;
    data.horizontalSpan = 2;
    targetProject.getCombo().setLayoutData(data);

    Label unitLabel = new Label(this, SWT.NONE);
    unitLabel.setText("Persistence");
    this.persistence = new ComboViewer(this, SWT.BORDER);
    GridData data3 = new GridData(768);
    data3.widthHint = 250;
    data3.horizontalSpan = 2;
    persistence.getCombo().setLayoutData(data3);
  }

  @SuppressWarnings("unused")
  public static void main(String[] args) {
    RootTestShell.run(ExcelUi::new);
  }
}
