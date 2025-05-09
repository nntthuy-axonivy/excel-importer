package com.axonivy.utils.excel.importer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import ch.ivyteam.ivy.IvyConstants;
import ch.ivyteam.ivy.designer.dialog.jsf.ui.JsfViewTechnologyDesignerUi;
import ch.ivyteam.ivy.designer.ui.view.ViewTechnologyDesignerUiRegistry;
import ch.ivyteam.ivy.dialog.configuration.DialogCreationParameters;
import ch.ivyteam.ivy.dialog.configuration.IUserDialog;
import ch.ivyteam.ivy.dialog.configuration.IUserDialogManager;
import ch.ivyteam.ivy.dialog.view.layout.ViewLayout;
import ch.ivyteam.ivy.process.model.element.activity.value.CallSignatureRef;
import ch.ivyteam.ivy.process.model.element.activity.value.dialog.UserDialogId;
import ch.ivyteam.ivy.process.model.element.activity.value.dialog.UserDialogStart;
import ch.ivyteam.ivy.process.model.element.event.start.value.CallSignature;
import ch.ivyteam.ivy.process.model.value.scripting.QualifiedType;
import ch.ivyteam.ivy.process.model.value.scripting.VariableDesc;
import ch.ivyteam.ivy.project.IIvyProject;
import ch.ivyteam.ivy.project.IvyProjectNavigationUtil;
import ch.ivyteam.ivy.scripting.dataclass.IEntityClass;
import ch.ivyteam.ivy.scripting.dataclass.IEntityClassField;
import ch.ivyteam.log.Logger;

public class DialogCreator {

  private static final Logger LOGGER = Logger.getLogger(DialogCreator.class);

  public IUserDialog createDialog(IEntityClass entity, String unit) {
    var global = IUserDialogManager.instance();
    IProject project = entity.getResource().getProject();
    var local = global.getProjectDataModelFor(project);

    var target = dialogStartFor(entity);

    var entries = new VariableDesc("entries", new QualifiedType(List.class.getName(), List.of(new QualifiedType(entity.getName()))));
    var edit = new VariableDesc("edit", new QualifiedType(entity.getName()));
    var editing = new VariableDesc("editing", new QualifiedType(Boolean.class.getName()));

    prepareTemplate(project, "frame-10");
    String dialogId = target.getId().getRawId();
    var params = new DialogCreationParameters.Builder(project, dialogId)
        .viewTechId(IvyConstants.ViewTechnology.JSF)
        .signature(new CallSignature(target.getStartMethod().getName()))
        .dataClassFields(List.of(entries, edit, editing))
        .toCreationParams();
    var userDialog = local.createProjectUserDialog(params);

    var processRdm = userDialog.getProcess();
    new DialogProcess(processRdm.getModel(), entity, unit).extendProcess();
    processRdm.save();

    extendView(userDialog.getViewFile(), entity);
    detailView(userDialog, entity);

    return userDialog;
  }

  private void detailView(IUserDialog userDialog, IEntityClass entity) {
    String template = readTemplate("EntityDetail.xhtml");

    String rendered = renderFields(entity, template, this::renderDetail);
    rendered = rendered.replaceAll("action=\"EntityList\"",
        "action=\"%s\"".formatted(entity.getSimpleName() + "Manager"));

    rendered = rendered.replace(
        "<p:commandLink id=\"cancel\" ",
        "<p:commandLink id=\"cancel\" actionListener=\"#{logic.cancel}\" ");

    var dir = (IFolder) userDialog.getResource();
    var detailView = dir.getFile("EntityDetail.xhtml");
    try (InputStream bis = new ByteArrayInputStream(rendered.getBytes())) {
      detailView.create(bis, true, null);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to write detail view " + detailView, ex);
    }
  }

  private void prepareTemplate(IProject project, String template) {
    try {
      var view = (JsfViewTechnologyDesignerUi) ViewTechnologyDesignerUiRegistry.getViewTechnology(IvyConstants.ViewTechnology.JSF);
      IIvyProject ivyProject = IvyProjectNavigationUtil.getIvyProject(project);
      List<ViewLayout> layouts = view.getViewLayoutProvider().getViewLayouts(ivyProject.project());
      Optional<ViewLayout> framed = layouts.stream().filter(l -> l.getLayoutName().contains("2 Column")).findFirst();
      framed.get().getViewContent("nevermind", template, List.of()); // just load to web-content
    } catch (Throwable ex) {
      LOGGER.error("Failed to prepare dialog template", ex);
    }
  }

  private void extendView(IFile viewFile, IEntityClass entity) {
    String template = readTemplate("EntityManager.xhtml");
    String rendered = renderFields(entity, template, this::renderColumn);
    write(viewFile, rendered);
  }

  private static String readTemplate(String resource) {
    try (InputStream is = DialogCreator.class.getResourceAsStream("/com/axonivy/utils/excel/importer/EntityManager/" + resource)) {
      var bos = new ByteArrayOutputStream();
      is.transferTo(bos);
      return new String(bos.toByteArray());
    } catch (Exception ex) {
      throw new RuntimeException("Failed to read template " + resource);
    }
  }

  private static void write(IFile view, String content) {
    try (var bis = new ByteArrayInputStream(content.getBytes())) {
      view.setContents(bis, 0, null);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to extend view for " + view, ex);
    }
  }

  private String renderFields(IEntityClass entity, String template, Function<IEntityClassField, String> renderer) {
    String fieldXhtml = entity.getFields().stream()
        .filter(fld -> !"id".equals(fld.getName()))
        .map(renderer)
        .collect(Collectors.joining("\n"));
    return template.replace("<!-- [entity.fields] -->", fieldXhtml);
  }

  private String renderColumn(IEntityClassField field) {
    return """
          <p:column headerText="%s">
            <h:outputText value="#{entity.%s}"/>
          </p:column>
      """.formatted(field.getComment(), field.getName());
  }

  private String renderDetail(IEntityClassField field) {
    return """
          <p:outputLabel for="FIELD" value="LABEL" />
          <p:inputText id="FIELD" value="#{data.edit.FIELD}"></p:inputText>
      """
        .replaceAll("FIELD", field.getName())
        .replaceAll("LABEL", field.getComment());
  }

  public static UserDialogStart dialogStartFor(IEntityClass entity) {
    var dialogId = UserDialogId.create(entity.getName() + "Manager");
    return new UserDialogStart(dialogId, new CallSignatureRef("start"));
  }

}
