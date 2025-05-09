package com.axonivy.utils.excel.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.axonivy.utils.excel.importer.EntityClassReader;
import com.axonivy.utils.excel.importer.ExcelLoader;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.scripting.dataclass.IEntityClassField;
import ch.ivyteam.ivy.scripting.streamInOut.DataClassAnnotation;

@IvyTest
public class TestEntityClassCreator {

  private EntityClassReader reader;

  @Test
  void readToEntity(@TempDir Path dir) throws IOException {
    Path path = dir.resolve("customers.xlsx");
    TstRes.loadTo(path, "sample.xlsx");

    var entity = reader.getEntity(path);
    assertThat(entity).isNotNull();
  }

  @Test
  void readGermanized(@TempDir Path dir) throws Exception {
    Path path = dir.resolve("Arzneimittel.xlsx");
    TstRes.loadTo(path, "ArzneimittelLight.xlsx");

    var entity = reader.getEntity(path);
    assertThat(entity).isNotNull();
    List<String> fields = entity.getFields().stream().map(IEntityClassField::getName).toList();
    for (String field : fields) {
      assertThat(field)
          .as("no whitespaces")
          .doesNotContain(" ")
          .doesNotContain("(")
          .doesNotContain("ä");
    }
    assertThat(entity.getField("anzahlInneresBehltnis").getComment())
        .as("preserve real column names")
        .isEqualTo("Anzahl Inneres Behältnis");

    assertThat(entity.getField("zulassungsinhaberName").getType())
        .isEqualTo(String.class.getName());
    assertThat(entity.getField("pNRZulassungsinhaber").getType())
        .isEqualTo(Integer.class.getName());
  }

  @Test
  void loadDataToEntity(@TempDir Path dir) throws Exception {
    Path path = dir.resolve("customers.xlsx");
    TstRes.loadTo(path, "sample.xlsx");

    Workbook wb = ExcelLoader.load(path);
    Sheet customerSheet = wb.getSheetAt(0);

    var customer = reader.toEntity(customerSheet, "customer");
    var field = (IEntityClassField) customer.getField("id");
    assertThat(field.getAnnotations())
        .extracting(DataClassAnnotation::fullAnnotation)
        .as("postgres ID generator in sequence; with drawback of slow batch-mode")
        .contains("@javax.persistence.GeneratedValue(strategy=javax.persistence.GenerationType.IDENTITY)");
  }

  @BeforeEach
  void setup() {
    this.reader = new EntityClassReader();
  }

}
