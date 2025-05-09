package com.axonivy.utils.excel.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.axonivy.utils.excel.importer.DialogCreator;
import com.axonivy.utils.excel.importer.EntityClassReader;
import com.axonivy.utils.excel.importer.ExcelLoader;

import ch.ivyteam.ivy.dialog.configuration.IUserDialog;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.activity.Script;
import ch.ivyteam.ivy.process.model.element.event.start.dialog.html.HtmlDialogEventStart;
import ch.ivyteam.ivy.process.model.element.event.start.dialog.html.HtmlDialogMethodStart;
import ch.ivyteam.ivy.process.model.element.value.Mapping;
import ch.ivyteam.ivy.process.model.element.value.Mappings;
import ch.ivyteam.ivy.scripting.dataclass.IDataClass;
import ch.ivyteam.ivy.scripting.dataclass.IDataClassField;
import ch.ivyteam.ivy.scripting.dataclass.IEntityClass;

@IvyTest
@SuppressWarnings("restriction")
public class TestDialogCreator {

  private EntityClassReader reader;

  @Test
  void createEntityDialog(@TempDir Path dir) throws IOException, CoreException {
    Path path = dir.resolve("customers.xlsx");
    TstRes.loadTo(path, "sample.xlsx");

    Workbook wb = ExcelLoader.load(path);
    Sheet customerSheet = wb.getSheetAt(0);

    IEntityClass customer = reader.toEntity(customerSheet, "customer");
    IUserDialog dialog = null;
    try {
      customer.save();

      String unit = "testing";
      dialog = new DialogCreator().createDialog(customer, unit);

      assertData(dialog.getDataClass());
      assertProcess(customer, dialog.getProcess().getModel());
      assertView(read(dialog.getViewFile()));
      var udRoot = (IFolder) dialog.getResource();
      assertDetailView(read(udRoot.getFile("EntityDetail.xhtml")));

    } finally {
      customer.getResource().delete(true, new NullProgressMonitor());
      if (dialog != null) {
        dialog.getResource().delete(true, null);
      }
    }
  }

  private void assertData(IDataClass dataClass) {
    assertThat(dataClass.getFields()).extracting(IDataClassField::getName)
        .containsOnly("entries", "edit", "editing");
  }

  private void assertProcess(IEntityClass customer, Process process) {
    Script loader = process.search().type(Script.class).findOne();
    assertThat(loader.getCode()).contains(customer.getName());

    var delete = process.search().type(HtmlDialogMethodStart.class).name("delete(customer)").findOne();
    String removal = delete.getParameterMappings().getCode();
    assertThat(removal)
        .contains("testing.remove(");

    var edit = process.search().type(HtmlDialogMethodStart.class).name("edit(customer)").findOne();
    Mappings mappings = edit.getParameterMappings().getMappings();
    assertThat(mappings.asList()).contains(
        new Mapping("out.edit", "param.entity"),
        new Mapping("out.editing", "true"));

    var save = process.search().type(HtmlDialogEventStart.class).name("save").findOne();
    assertThat(save.getOutput().getCode())
        .contains("ivy.persistence.testing.merge(in.edit)");

    var add = process.search().type(HtmlDialogEventStart.class).name("add").findOne();
    assertThat(add.getOutput().getMappings().asList().get(0).getRightSide())
        .contains("new " + customer.getName() + "()");
    assertThat(add.getOutput().getCode())
        .as("modify flag on 'in' to guarantee updates")
        .isEqualTo("in.editing = false;");

    var cancel = process.search().type(HtmlDialogEventStart.class).name("cancel").findOne();
    assertThat(cancel.getOutput().getCode())
        .contains("in.edit = null;");
  }

  private void assertView(String view) {
    assertThat(view).contains("p:dataTable");
    assertThat(view)
        .as("visualizes properties of the entity")
        .contains("firstname")
        .doesNotContain("<!-- [entity.fields] -->");
  }

  private void assertDetailView(String view) {
    assertThat(view)
        .as("visualizes properties of the entity")
        .contains("firstname")
        .doesNotContain("<!-- [entity.fields] -->");
    assertThat(view)
        .as("navigation to list must be adapted by the template renderer")
        .doesNotContain("action=\"EntityList\"");
  }

  private static String read(IFile viewFile) throws IOException, CoreException {
    try (InputStream in = viewFile.getContents()) {
      var bos = new java.io.ByteArrayOutputStream();
      in.transferTo(bos);
      return new String(bos.toByteArray());
    }
  }

  @BeforeEach
  void setup() {
    this.reader = new EntityClassReader();
  }

}
