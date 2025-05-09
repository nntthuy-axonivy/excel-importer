package com.axonivy.utils.excel.importer.wizard.swt;

import java.util.function.Function;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;

import ch.ivyteam.api.API;

public class SwtLabelProvider<T> extends BaseLabelProvider implements ILabelProvider {
  private final Function<T, String> displayTextProvider;
  private final Function<T, Image> imageProvider;
  private static final Function<?, Image> NULL_IMAGE_PROVIDER = value -> null;

  public SwtLabelProvider(Function<T, String> displayTextProvider) {
    this(displayTextProvider, nullImageProvider());
  }

  public SwtLabelProvider(Function<T, String> displayTextProvider, Function<T, Image> imageProvider) {
    API.checkParameterNotNull(displayTextProvider, "displayTextProvdier");
    API.checkParameterNotNull(imageProvider, "imageProvider");
    this.displayTextProvider = displayTextProvider;
    this.imageProvider = imageProvider;
  }

  @Override
  public String getText(Object element) {
    element = handleEmptyStringAsNull(element);
    return displayTextProvider.apply(toModel(element));
  }

  @Override
  public Image getImage(Object element) {
    element = handleEmptyStringAsNull(element);
    return imageProvider.apply(toModel(element));
  }

  private static Object handleEmptyStringAsNull(Object element) {
    return "".equals(element) ? null : element;
  }

  @SuppressWarnings("unchecked")
  private T toModel(Object element) {
    return (T)element;
  }

  @SuppressWarnings("unchecked")
  private static <T> Function<T, Image> nullImageProvider() {
    return (Function<T, Image>) NULL_IMAGE_PROVIDER;
  }

}
