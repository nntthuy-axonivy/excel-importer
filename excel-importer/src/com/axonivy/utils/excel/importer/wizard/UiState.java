package com.axonivy.utils.excel.importer.wizard;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.axonivy.utils.excel.importer.wizard.swt.SwtComboModelBinder;
import com.axonivy.utils.excel.importer.wizard.swt.UiComboModel;

import ch.ivyteam.ui.model.UiModelListener;
import ch.ivyteam.ui.model.UiTextModel;
import ch.ivyteam.ui.model.event.ValueChangeEvent;
import ch.ivyteam.ui.model.swt.SwtBinder;

public class UiState {

  public final UiComboModel<String> file;
  public final UiComboModel<String> project;
  public final UiTextModel entity;
  public final UiComboModel<String> persistence;

  public UiState(ExcelImportProcessor processor, Supplier<List<String>> history) {
    this.file = new UiComboModel<>(processor::getImportFile, processor::setImportFile, ()->history.get());
    this.entity = new UiTextModel(processor::getEntityName, processor::setEntityName);
    this.project = new UiComboModel<>(processor::getProjectName, processor::setProject,
      () -> ExcelImportUtil.getIvyProjectNames());
    this.persistence = new UiComboModel<>(processor::getPersistence, processor::setPersistence,
      () -> processor.units()).dependsOnValueOf(project);
  }

  public void bind(ExcelUi ui) {
    var binder = new SwtBinder();
    new SwtComboModelBinder<>(file).to(ui.importFile);
    binder.bind(entity).to(ui.entity);
    new SwtComboModelBinder<>(project).to(ui.targetProject);
    new SwtComboModelBinder<>(persistence).to(ui.persistence);
  }

  public void watch(Consumer<String> listener) {
    UiModelListener uiListener = evt -> {
      if (evt instanceof ValueChangeEvent change) {
        listener.accept((String)change.getValue());
      }
    };
    file.addUiModelListener(uiListener);
    project.addUiModelListener(uiListener);
    entity.addUiModelListener(uiListener);
    persistence.addUiModelListener(uiListener);
  }

}
