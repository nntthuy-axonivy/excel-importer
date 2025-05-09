package com.axonivy.utils.excel.importer;

import java.util.List;

import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.diagram.shape.DiagramShape;
import ch.ivyteam.ivy.process.model.diagram.value.Position;
import ch.ivyteam.ivy.process.model.diagram.value.PositionDelta;
import ch.ivyteam.ivy.process.model.element.activity.Script;
import ch.ivyteam.ivy.process.model.element.event.end.dialog.html.HtmlDialogEnd;
import ch.ivyteam.ivy.process.model.element.event.start.dialog.html.HtmlDialogEventStart;
import ch.ivyteam.ivy.process.model.element.event.start.dialog.html.HtmlDialogMethodStart;
import ch.ivyteam.ivy.process.model.element.event.start.dialog.html.HtmlDialogStart;
import ch.ivyteam.ivy.process.model.element.event.start.value.CallSignature;
import ch.ivyteam.ivy.process.model.element.value.Mapping;
import ch.ivyteam.ivy.process.model.element.value.Mappings;
import ch.ivyteam.ivy.process.model.value.MappingCode;
import ch.ivyteam.ivy.process.model.value.scripting.QualifiedType;
import ch.ivyteam.ivy.process.model.value.scripting.VariableDesc;
import ch.ivyteam.ivy.scripting.dataclass.IEntityClass;

public class DialogProcess {

  private final Process process;
  private final IEntityClass entity;
  private final String unit;

  private final int x = 96;
  private int y = 248;

  public DialogProcess(Process process, IEntityClass entity, String unit) {
    this.process = process;
    this.entity = entity;
    this.unit = unit;
  }

  public void extendProcess() {
    addDbLoaderScript();
    addDeleteAction();
    addEditAction();
    addCreateAction();
    addSaveAction();
    addCancelAction();
  }

  private void addDbLoaderScript() {
    var start = process.search().type(HtmlDialogStart.class).findOne();
    var startEnd = start.getOutgoing().get(0).getTarget();
    DiagramShape endShape = startEnd.getShape();
    endShape.move(new PositionDelta(200, 0)); // space for loader script

    Script loader = process.add().element(Script.class);
    loader.setName("load db");
    loader.setCode("""
      out.entries = ivy.persistence.%s.findAll(%s.class);
      """.formatted(unit, entity.getName()));

    DiagramShape scriptShape = loader.getShape();
    scriptShape.moveTo(start.getShape().getBounds().getCenter().shiftX(150));
    process.connections().reconnect(start.getOutgoing().get(0)).to(loader);

    scriptShape.edges().connectTo(endShape);
  }

  private void addDeleteAction() {
    var delete = addMethod();
    delete.setName("delete(" + entity.getSimpleName() + ")");
    var param = new VariableDesc("entity", new QualifiedType(entity.getName()));
    delete.setSignature(new CallSignature("delete").setInputParameters(List.of(param)));

    var template = """
      import NAME;
      SHORT loaded = ivy.persistence.UNIT.find(SHORT.class, param.entity.getId()) as SHORT;
      ivy.persistence.UNIT.remove(loaded);
      out.entries.remove(param.entity);
      """;
    var code = template
        .replaceAll("NAME", entity.getName())
        .replaceAll("SHORT", entity.getSimpleName())
        .replaceAll("UNIT", unit);

    delete.setParameterMappings(new MappingCode(code));
  }

  private void addEditAction() {
    var edit = addMethod();
    edit.setName("edit(" + entity.getSimpleName() + ")");
    var param = new VariableDesc("entity", new QualifiedType(entity.getName()));
    edit.setSignature(new CallSignature("edit").setInputParameters(List.of(param)));

    edit.setParameterMappings(new MappingCode(new Mappings(
        new Mapping("out.edit", "param.entity"),
        new Mapping("out.editing", "true"))));
  }

  private void addCreateAction() {
    var add = addEvent();
    add.setName("add");
    add.setOutput(new MappingCode(new Mappings(
        new Mapping("out.edit", "new " + entity.getName() + "()")),
        "in.editing = false;"));
  }

  private void addSaveAction() {
    var save = addEvent();
    save.setName("save");
    String doSave = """
      ivy.log.debug("edit="+in.editing+ " /what="+in.edit);
      if (in.editing) {
        ivy.persistence.UNIT.merge(in.edit);
      } else {
        in.edit = ivy.persistence.UNIT.persist(in.edit) as ENTITY;
        in.entries.add(in.edit);
      }
      in.edit = null;
      """
        .replaceAll("UNIT", unit)
        .replaceAll("ENTITY", entity.getName());
    save.setOutput(new MappingCode(doSave));
  }

  private void addCancelAction() {
    var cancel = addEvent();
    cancel.setName("cancel");
    var doCancel = """
      ivy.log.debug("canceling edit="+in.editing+ " /what="+in.edit);
      if (in.editing) {
        ivy.persistence.UNIT.refresh(in.edit);
      }
      in.edit = null;
      """
        .replaceAll("UNIT", unit);
    cancel.setOutput(new MappingCode(doCancel));
  }

  private HtmlDialogMethodStart addMethod() {
    return addAction(HtmlDialogMethodStart.class);
  }

  private HtmlDialogEventStart addEvent() {
    return addAction(HtmlDialogEventStart.class);
  }

  private HtmlDialogEnd addEndFor(NodeElement other) {
    HtmlDialogEnd end = process.add().element(HtmlDialogEnd.class);
    Position center = other.getShape().getBounds().getCenter();
    end.getShape().moveTo(center.shiftX(100));
    other.getShape().edges().connectTo(end.getShape());
    return end;
  }

  private <T extends NodeElement> T addAction(Class<T> type) {
    var action = process.add().element(type);
    action.getShape().moveTo(new Position(x, y));
    y += 80;
    addEndFor(action);
    return action;
  }

}
