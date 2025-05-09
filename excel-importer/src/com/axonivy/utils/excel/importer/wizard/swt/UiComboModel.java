package com.axonivy.utils.excel.importer.wizard.swt;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import ch.ivyteam.api.API;
import ch.ivyteam.ui.model.UiValueWidgetModel;

public class UiComboModel<T> extends UiValueWidgetModel<UiComboModel<T>, T> {
  private Supplier<List<T>> valuesSupplier;
  private Function<T, String> displayTextProvider;

  public UiComboModel(Supplier<T> supplier, Consumer<T> consumer, List<T> values) {
    this(supplier, consumer, () -> values);
  }

  public UiComboModel(Supplier<T> supplier, Consumer<T> consumer, T[] values) {
    this(supplier, consumer, ()->values);
  }

  public UiComboModel(Supplier<T> supplier, Consumer<T> consumer, T defaultValue) {
    super(supplier, consumer, defaultValue);
  }

  public UiComboModel(Supplier<T> supplier, Consumer<T> consumer, ListSupplier<T> valuesSupplier) {
    super(supplier, consumer, getFirstAsDefaultValue(valuesSupplier.get()));
    this.valuesSupplier = valuesSupplier;
  }

  public UiComboModel(Supplier<T> supplier, Consumer<T> consumer, Supplier<T[]> valuesSupplier) {
    this(supplier, consumer, () -> Arrays.asList(valuesSupplier.get()));
  }

  public List<T> getValues() {
    if (valuesSupplier == null) {
      return null;
    }
    return valuesSupplier.get();
  }

  public T getSelection() {
    return getValue();
  }

  public void setSelection(T selection) {
    setValue(selection);
  }

  @Override
  public UiComboModel<T> withDefaultValue(T defaultValue) {
    return super.withDefaultValue(defaultValue);
  }

  public UiComboModel<T> withDisplayTextProvider(@SuppressWarnings("hiding") Function<T, String> displayTextProvider) {
    API.checkParameterNotNull(displayTextProvider,  "displayTextProvider");
    this.displayTextProvider = displayTextProvider;
    return this;
  }

  public Function<T, String> getDisplayTextProvider() {
    return displayTextProvider;
  }

  private static <T> T getFirstAsDefaultValue(List<T> values) {
    return values.stream()
            .map(Optional::ofNullable).findFirst().flatMap(Function.identity())
            .orElse(null);
  }

  public interface ListSupplier<T> extends Supplier<List<T>> {
  }

}