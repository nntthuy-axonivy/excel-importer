package com.axonivy.utils.excel.importer.wizard;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.axonivy.utils.excel.importer.DialogCreator;
import com.axonivy.utils.excel.importer.EntityClassReader;
import com.axonivy.utils.excel.importer.EntityDataLoader;
import com.axonivy.utils.excel.importer.ExcelLoader;
import com.axonivy.utils.excel.importer.ProcessDrawer;

import ch.ivyteam.awt.swt.SwtRunnable;
import ch.ivyteam.eclipse.util.EclipseUtil;
import ch.ivyteam.eclipse.util.MonitorUtil;
import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.designer.ui.wizard.restricted.IWizardSupport;
import ch.ivyteam.ivy.designer.ui.wizard.restricted.WizardStatus;
import ch.ivyteam.ivy.eclipse.util.EclipseUiUtil;
import ch.ivyteam.ivy.process.data.persistence.PersistenceContextFactory;
import ch.ivyteam.ivy.process.data.persistence.datamodel.IProcessDataPersistenceConfigManager;
import ch.ivyteam.ivy.process.data.persistence.model.Persistence.PersistenceUnit;
import ch.ivyteam.ivy.project.IIvyProject;
import ch.ivyteam.ivy.project.IIvyProjectManager;
import ch.ivyteam.ivy.request.RequestFactory;
import ch.ivyteam.ivy.request.impl.RequestContext;
import ch.ivyteam.ivy.scripting.dataclass.IDataClassManager;
import ch.ivyteam.ivy.scripting.dataclass.IEntityClass;
import ch.ivyteam.ivy.scripting.dataclass.IProjectDataClassManager;
import ch.ivyteam.ivy.search.restricted.ProjectRelationSearchScope;
import ch.ivyteam.log.Logger;
import ch.ivyteam.util.io.resource.FileResource;
import ch.ivyteam.util.io.resource.nio.NioFileSystemProvider;

public class ExcelImportProcessor implements IWizardSupport, IRunnableWithProgress {

  private static final Logger LOGGER = Logger.getLogger(ExcelImportProcessor.class);

  private IIvyProject selectedSourceProject;
  private FileResource importFile;
  private IStatus status = Status.OK_STATUS;

  private String selectedPersistence = "";
  private String entityName = "";
  private String projectName = "";
  private String file = "";

  public ExcelImportProcessor(IStructuredSelection selection) {
    this.selectedSourceProject = ExcelImportUtil.getFirstNonImmutableIvyProject(selection);
  }

  @Override
  public String getWizardPageTitle(String pageId) {
    return "Import Excel as Dialog";
  }

  @Override
  public String getWizardPageOkMessage(String pageId) {
    return "Please specify an Excel file to import as Dialog";
  }

  @Override
  public boolean wizardFinishInvoked() {
    var okStatus = WizardStatus.createOkStatus();
    okStatus.merge(validateImportFileExits());
    okStatus.merge(validateProject());
    return okStatus.isLowerThan(WizardStatus.ERROR);
  }

  @Override
  public boolean wizardCancelInvoked() {
    return true;
  }

  @Override
  public void run(IProgressMonitor monitor) throws InvocationTargetException {
    SubMonitor progress = MonitorUtil.begin(monitor, "Importing", 1);
    try {
      status = Status.OK_STATUS;
      ResourcesPlugin.getWorkspace().run(m -> {
        var manager = IDataClassManager.instance().getProjectDataModelFor(selectedSourceProject.getProject());
        try {
          importExcel(manager, importFile, m);
        } catch (Exception ex) {
          status = EclipseUtil.createErrorStatus(ex);
        }
      }, null, IWorkspace.AVOID_UPDATE, progress);
    } catch (Exception ex) {
      status = EclipseUtil.createErrorStatus(ex);
    } finally {
      MonitorUtil.ensureDone(monitor);
    }
  }

  private void importExcel(IProjectDataClassManager manager, FileResource excel, IProgressMonitor monitor) throws Exception {
    Workbook wb = null;
    try (InputStream is = excel.read().inputStream()) {
      wb = ExcelLoader.load(excel.name(), excel.read().inputStream());
    }
    Sheet sheet = wb.getSheetAt(0);

    var newEntity = new EntityClassReader(manager).toEntity(sheet, entityName);
    newEntity.save();
    SwtRunnable.execNowOrAsync(() -> EclipseUiUtil.openEditor(newEntity));
    monitor.setTaskName("Created EntityClass " + entityName);

    IProcessModelVersion pmv = manager.getProcessModelVersion();
    int loaded = 0;
    try {
      var entries = importData(sheet, newEntity, pmv);
      loaded = entries.size();
    } catch (Exception ex) {
      LOGGER.error("Excel data import failed", ex);
      status = EclipseUtil.createErrorStatus("Loading of Excel data failed", ex);
    }
    monitor.setTaskName("Loaded Excel rows into Database " + loaded);

    new DialogCreator().createDialog(newEntity, selectedPersistence);

    ProcessDrawer drawer = new ProcessDrawer(manager.getProject());
    var process = drawer.drawManager(newEntity);
    SwtRunnable.execNowOrAsync(() -> EclipseUiUtil.openEditor(process));
  }

  private List<?> importData(Sheet sheet, IEntityClass newEntity, IProcessModelVersion pmv) throws Exception {
    var persist = PersistenceContextFactory.of(pmv);
    var ivyEntities = persist.get(selectedPersistence);
    EntityDataLoader loader = new EntityDataLoader(ivyEntities);

    var system = pmv.getApplication().getSecurityContext().sessions().systemUser();
    var request = RequestFactory.createRestRequest(pmv, system);
    return new RequestContext(request).callInContext(() -> {
      var entityType = loader.createTable(newEntity);
      loader.load(sheet, newEntity);
      return ivyEntities.findAll(entityType);
    });
  }

  String getSelectedSourceProjectName() {
    if (selectedSourceProject == null) {
      return StringUtils.EMPTY;
    }
    return selectedSourceProject.getName();
  }

  public String getImportFile() {
    return file;
  }

  public WizardStatus setImportFile(String text) {
    this.file = text;
    if (text != null) {
      try {
        importFile = NioFileSystemProvider.create(Path.of("/")).root().file(text);
      } catch (Exception ex) {
        return WizardStatus.createErrorStatus("Can't create file from " + text, ex);
      }
    } else {
      importFile = null;
    }
    return validateImportFileExits();
  }

  public String getEntityName() {
    return entityName;
  }

  public WizardStatus setEntityName(String name) {
    this.entityName = name;
    return validateEntity();
  }

  public WizardStatus validateEntity() {
    if (entityName.isBlank()) {
      return WizardStatus.createErrorStatus("Need a valid name for the Data to import");
    }
    return WizardStatus.createOkStatus();
  }

  public String getProjectName() {
    return this.projectName;
  }

  public WizardStatus setProject(String projectName) {
    if (projectName != null && !Objects.equals(projectName, this.projectName)) {
      selectedSourceProject = IIvyProjectManager.instance().getIvyProject(projectName);
    } else {
      selectedSourceProject = null;
    }
    this.projectName = projectName;
    return validateProject();
  }

  public String getPersistence() {
    return selectedPersistence;
  }

  public WizardStatus setPersistence(String name) {
    selectedPersistence = name;
    return validatePersistence();
  }

  public IStatus getStatus() {
    return status;
  }

  public WizardStatus validateImportFileExits() {
    if (importFile == null || !importFile.exists()) {
      return WizardStatus.createErrorStatus("Import file " + importFile + " does not exist");
    }
    return WizardStatus.createOkStatus();
  }

  public WizardStatus validateProject() {
    if (selectedSourceProject == null) {
      return WizardStatus.createErrorStatus("Please specify an Axon Ivy project");
    }
    return WizardStatus.createOkStatus();
  }

  public WizardStatus validatePersistence() {
    if (selectedPersistence == null || !units().contains(selectedPersistence)) {
      return WizardStatus.createErrorStatus("Please specify a Persistence DB to store XLS data");
    }
    return WizardStatus.createOkStatus();
  }

  public List<String> units() {
    if (selectedSourceProject == null) {
      return List.of();
    }
    var main = IProcessDataPersistenceConfigManager.instance();
    var local = main.getProjectDataModelFor(selectedSourceProject.getProject());
    return local.getDataModels(ProjectRelationSearchScope.CURRENT_AND_ALL_REQUIRED_PROJECTS)
        .getModels().stream()
        .flatMap(c -> c.getPersistenceUnitConfigs().stream())
        .map(PersistenceUnit::getName)
        .toList();
  }
}
