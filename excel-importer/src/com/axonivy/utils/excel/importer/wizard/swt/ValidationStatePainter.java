package com.axonivy.utils.excel.importer.wizard.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Control;

import ch.ivyteam.ui.model.validation.ValidationState;

public class ValidationStatePainter implements PaintListener {
  private ValidationState validationState;
  private Control control;

  public ValidationStatePainter(Control control) {
    this.control = control;
    this.control.addPaintListener(this);
  }

  public void setValidationState(ValidationState validationState) {
    this.validationState = validationState;
    control.redraw();
  }

  @Override
  public void paintControl(PaintEvent e) {
    if (validationState.hasErrors()) {
      paintControl(e, SWT.COLOR_RED);
    } else if (validationState.hasWarnings()) {
      paintControl(e, SWT.COLOR_YELLOW);
    }
  }

  private void paintControl(PaintEvent e, int color) {
    int x1 = e.x+2;
    int x2 = e.x+e.width-4;
    int y = e.y + e.height-3;
    e.gc.setForeground(e.widget.getDisplay().getSystemColor(color));
    e.gc.setLineDash(new int[] {1,1});
    e.gc.drawLine(x1+1, y-1, x2, y-1);
    e.gc.setLineDash(new int[] {1,3});
    e.gc.drawLine(x1, y, x2, y);
    e.gc.drawLine(x1+2, y-2, x2, y-2);
  }

}
