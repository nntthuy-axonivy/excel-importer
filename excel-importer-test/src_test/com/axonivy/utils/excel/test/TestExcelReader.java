package com.axonivy.utils.excel.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.axonivy.utils.excel.importer.Column;
import com.axonivy.utils.excel.importer.ExcelLoader;
import com.axonivy.utils.excel.importer.ExcelReader;

class TestExcelReader {

  @Test
  void parseColumns_xlsx(@TempDir Path dir) throws IOException {
    Path path = dir.resolve("customers.xlsx");
    TstRes.loadTo(path, "sample.xlsx");
    Workbook wb = ExcelLoader.load(path);
    List<Column> columns = ExcelReader.parseColumns(wb.getSheetAt(0));
    assertThat(columns).extracting(Column::getName)
        .contains("Firstname", "Lastname");
    assertThat(columns).contains(
        new Column("Firstname", String.class, 255), new Column("ZIP", Integer.class),
        new Column("Amount", Double.class), new Column("Birthdate", Date.class), // should be a date
        new Column("Note", String.class, 255),
        new Column("Column contains texts in incorrect number format", String.class, 255),
        new Column("Column contains both text and numeric", String.class, 255));
  }

  @Test
  void parseColumnsOver255Characters_xlsx(@TempDir Path dir) throws IOException {
    Path path = dir.resolve("customers.xlsx");
    TstRes.loadTo(path, "sample_over_255_characters.xlsx");
    Workbook wb = ExcelLoader.load(path);
    List<Column> columns = ExcelReader.parseColumns(wb.getSheetAt(0));
    assertThat(columns).extracting(Column::getName).contains("FirstName", "LastName", "Summary");
    assertThat(columns).contains(new Column("FirstName", String.class, 255),
        new Column("LastName", String.class, 255),
        new Column("Summary", String.class, 823));
  }

  @Test
  void parseColumnsSeveralDateFormats_xlsx(@TempDir Path dir) throws IOException {
    Path path = dir.resolve("customers.xlsx");
    TstRes.loadTo(path, "sample_date_format.xlsx");
    Workbook wb = ExcelLoader.load(path);
    List<Column> columns = ExcelReader.parseColumns(wb.getSheetAt(0));
    assertThat(columns).extracting(Column::getName).contains("Start date", "End date", "Date without year");
    assertThat(columns).contains(new Column("Start date", Date.class),
        new Column("End date", Date.class), new Column("Date without year", Date.class));
  }

}
