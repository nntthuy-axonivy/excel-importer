package com.axonivy.utils.excel.importer;

import java.nio.file.Path;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.scripting.dataclass.DataClassFieldModifier;
import ch.ivyteam.ivy.scripting.dataclass.IDataClassManager;
import ch.ivyteam.ivy.scripting.dataclass.IEntityClass;
import ch.ivyteam.ivy.scripting.dataclass.IEntityClassField;
import ch.ivyteam.ivy.scripting.dataclass.IProjectDataClassManager;
import ch.ivyteam.ivy.scripting.streamInOut.DataClassAnnotation;

public class EntityClassReader {

  public final IProjectDataClassManager manager;

  public EntityClassReader() {
    this(IDataClassManager.instance().getProjectDataModelFor(Ivy.wfCase().getProcessModelVersion()));
  }

  public EntityClassReader(IProjectDataClassManager manager) {
    this.manager = manager;
  }

  public IEntityClass getEntity(Path filePath) {
    Workbook wb = ExcelLoader.load(filePath);
    String dataName = StringUtils.substringBeforeLast(filePath.getFileName().toString(), ".");
    dataName = StringUtils.capitalize(dataName);
    return toEntity(wb.getSheetAt(0), dataName);
  }

  public IEntityClass toEntity(Sheet sheet, String dataName) {
    String fqName = manager.getDefaultNamespace() + "." + dataName;
    if (manager.findDataClass(fqName) != null) {
      throw new RuntimeException("entity " + fqName + " already exists");
    }
    var entity = manager.createEntityClass(fqName);

    withIdField(entity);
    ExcelReader.parseColumns(sheet).stream().forEachOrdered(col -> {
      var field = entity.addField(fieldName(col.getName()), col.getType().getName());
      field.setComment(col.getName());
      field.setDatabaseFieldLength(String.valueOf(col.getDatabaseFieldLength()));
    });
    return entity;
  }

  private void withIdField(IEntityClass entity) {
    IEntityClassField id = entity.addField("id", Integer.class.getSimpleName());
    id.setDatabaseFieldName("id");
    id.addModifier(DataClassFieldModifier.ID);
    id.addModifier(DataClassFieldModifier.PERSISTENT);
    appendIdentityStrategy(id);
    id.setComment("Identifier");
  }

  private static void appendIdentityStrategy(IEntityClassField id) {
    id.addAnnotation(new DataClassAnnotation(
        "@" + GeneratedValue.class.getName() + "(strategy="
            + GenerationType.class.getName() + "." + GenerationType.IDENTITY.name()
            + ")"));
  }

  private String fieldName(String colName) {
    colName = colName.replace(" ", "");
    if (StringUtils.isAllUpperCase(colName)) {
      return colName.toLowerCase();
    }
    colName = colName.replaceAll("\\W", "");
    colName = colName.replaceAll("[^\\p{ASCII}]", "");
    return StringUtils.uncapitalize(colName);
  }

}
