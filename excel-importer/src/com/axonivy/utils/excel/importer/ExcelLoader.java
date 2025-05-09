package com.axonivy.utils.excel.importer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelLoader {

  public static Workbook load(Path filePath) {
    try (InputStream is = Files.newInputStream(filePath)) {
      return load(filePath.getFileName().toString(), is);
    } catch (Exception ex) {
      throw new RuntimeException("Could not read excel file from " + filePath, ex);
    }
  }

  public static Workbook load(String name, InputStream stream) {
    try {
      if (name.endsWith(".xls")) {
        return openXls(stream);
      } else {
        return openXlsx(stream);
      }
    } catch (IOException ex) {
      throw new RuntimeException("Could not read excel file from " + name, ex);
    }
  }

  private static Workbook openXls(InputStream is) throws IOException {
    try ( POIFSFileSystem fs = new POIFSFileSystem(is);
          HSSFWorkbook workbook = new HSSFWorkbook(fs);) {
      return workbook;
    }
  }

  private static Workbook openXlsx(InputStream is) throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook(is);) {
      return workbook;
    }
  }

}