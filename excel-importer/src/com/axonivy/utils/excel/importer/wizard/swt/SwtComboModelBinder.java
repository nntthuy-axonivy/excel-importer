package com.axonivy.utils.excel.importer.wizard.swt;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Combo;

import ch.ivyteam.swt.SwtSelectionUtil;
import ch.ivyteam.ui.model.swt.SwtModelBinder;
import ch.ivyteam.ui.model.swt.SwtValueControlBinder;

public class SwtComboModelBinder<T> extends SwtModelBinder<UiComboModel<T>> {
  private ILabelProvider labelProvider;

  public SwtComboModelBinder(UiComboModel<T> model) {
    super(model);
  }

  public SwtComboBinder<T> to(ComboViewer combo) {
    return new SwtComboBinder<T>(model).to(combo, labelProvider);
  }

  public SwtComboModelBinder<T> withLabelProvider(@SuppressWarnings("hiding") ILabelProvider labelProvider) {
    this.labelProvider = labelProvider;
    return this;
  }

  public static class SwtComboBinder<T> extends SwtValueControlBinder<SwtComboBinder<T>, UiComboModel<T>, Combo> {
    private ComboViewer uiViewer;
    private ValidationStatePainter validationStatePainter;

    public SwtComboBinder(UiComboModel<T> model) {
      super(model);
    }

    public SwtComboBinder<T> to(ComboViewer combo, ILabelProvider labelProvider) {
      super.to(combo.getCombo());
      validationStatePainter = new ValidationStatePainter(combo.getCombo());
      this.uiViewer = combo;

      if (labelProvider != null) {
        uiViewer.setLabelProvider(labelProvider);
      } else if (model.getDisplayTextProvider() != null) {
        uiViewer.setLabelProvider(new SwtLabelProvider<T>(model.getDisplayTextProvider()));
      }
      if (uiViewer.getContentProvider() == null) {
        uiViewer.setContentProvider(ArrayContentProvider.getInstance());
      }
      setValuesToUi();

      valueChanged();
      validationChanged();
      combo.addSelectionChangedListener(this::uiChanged);
      return this;
    }

    @Override
    public void updateUiFromModel() {
      T selection = model.getSelection();
      Object uiSelection = toUiValue(selection);

      setValuesToUi();
      StructuredSelection structuredSelection = new StructuredSelection(uiSelection);
      uiViewer.setSelection(structuredSelection);
    }

    private void setValuesToUi() {
      Object[] values = getValues();
      if (values != null) {
        uiViewer.setInput(values);
      }
    }

    @Override
    public void validationChanged() {
      validationStatePainter.setValidationState(model.getValidationState());
    }

    private Object[] getValues() {
      List<T> modelValues = model.getValues();
      if (modelValues == null) {
        return null;
      }
      if (!hasNullValues(modelValues)) {
        return modelValues.toArray();
      }

      return modelValues.stream().map(this::toUiValue).toArray();
    }

    private static boolean hasNullValues(List<?> values) {
      for (int i = 0; i < values.size(); i++) {
        if (values.get(i) == null) {
          return true;
        }
      }
      return false;
    }

    private void uiChanged(SelectionChangedEvent event) {
      if (!SwtSelectionUtil.isEmpty(event.getSelection())) {
        Object selection = SwtSelectionUtil.getFirstElement(event.getSelection());
        T modelSelection = toModelValue(selection);
        updateModelFromUi(()->model.setSelection(modelSelection));
      }
    }

    private Object toUiValue(T modelValue) {
      return modelValue != null ? modelValue : StringUtils.EMPTY;
    }

    @SuppressWarnings("unchecked")
    private T toModelValue(Object uiValue) {
      return uiValue != StringUtils.EMPTY ? (T)uiValue : null;
    }
  }
}
