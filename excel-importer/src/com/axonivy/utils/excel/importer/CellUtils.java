package com.axonivy.utils.excel.importer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

public class CellUtils {
  public static boolean isInteger(Cell cell) {
    if (cell == null || cell.getCellType() != CellType.NUMERIC) {
      return false;
    }
    double cellValue = cell.getNumericCellValue();
    return cellValue == (int) cellValue;
  }

}
