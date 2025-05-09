package com.axonivy.utils.excel.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.axonivy.utils.excel.importer.EntityClassReader;
import com.axonivy.utils.excel.importer.EntityDataLoader;
import com.axonivy.utils.excel.importer.ExcelLoader;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.data.persistence.IIvyEntityManager;
import ch.ivyteam.ivy.scripting.dataclass.IEntityClass;

@IvyTest
@SuppressWarnings("restriction")
public class TestEntityDataLoader {

  private EntityDataLoader loader;
  private EntityClassReader reader;
  private IIvyEntityManager unit;

  @Test
  void loadDataToEntity(@TempDir Path dir) throws Exception {
    Path path = dir.resolve("customers.xlsx");
    TstRes.loadTo(path, "sample.xlsx");

    Workbook wb = ExcelLoader.load(path);
    Sheet customerSheet = wb.getSheetAt(0);

    IEntityClass customer = reader.toEntity(customerSheet, "customer");
    try {
      customer.save();
      Class<?> entity = loader.createTable(customer);
      assertThat(unit.findAll(entity)).isEmpty();
      loader.load(customerSheet, customer);
      List<?> records = unit.findAll(entity);
      assertThat(records).hasSize(2);
    } finally {
      customer.getResource().delete(true, new NullProgressMonitor());
    }
  }

  @Test
  void loadArznei(@TempDir Path dir) throws Exception {
    Path path = dir.resolve("meds.xlsx");
    TstRes.loadTo(path, "ArzneimittelLight.xlsx");

    Workbook wb = ExcelLoader.load(path);
    Sheet medSheet = wb.getSheetAt(0);

    IEntityClass meds = reader.toEntity(medSheet, "meds");
    try {
      meds.save();
      Class<?> entity = loader.createTable(meds);
      assertThat(unit.findAll(entity)).isEmpty();
      loader.load(medSheet, meds);
      List<?> records = unit.findAll(entity);
      assertThat(records).hasSizeGreaterThanOrEqualTo(2);
    } finally {
      meds.getResource().delete(true, new NullProgressMonitor());
    }
  }

  @BeforeEach
  void setup() {
    this.unit = Ivy.persistence().get("testing");
    unit.createEntityManager().clear(); // eager access
    this.loader = new EntityDataLoader(unit);
    this.reader = new EntityClassReader();
  }

}
