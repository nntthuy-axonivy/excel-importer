package com.axonivy.utils.excel.importer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.internal.SessionImpl;

import ch.ivyteam.ivy.java.IJavaConfigurationManager;
import ch.ivyteam.ivy.process.data.persistence.IIvyEntityManager;
import ch.ivyteam.ivy.project.IIvyProjectManager;
import ch.ivyteam.ivy.scripting.dataclass.IEntityClass;
import ch.ivyteam.ivy.scripting.dataclass.IEntityClassField;
import ch.ivyteam.log.Logger;

public class EntityDataLoader {

  private static final Logger LOGGER = Logger.getLogger(EntityDataLoader.class);
  private final IIvyEntityManager manager;

  public EntityDataLoader(IIvyEntityManager manager) {
    this.manager = manager;
  }

  public void load(Sheet sheet, IEntityClass entity) throws SQLException {
    load(sheet, entity, new NullProgressMonitor());
  }

  public void load(Sheet sheet, IEntityClass entity, IProgressMonitor monitor) throws SQLException {
    Iterator<Row> rows = sheet.rowIterator();
    rows.next(); // skip header
    monitor.beginTask("Importing Excel data rows", sheet.getLastRowNum());

    EntityManager em = manager.createEntityManager();
    JdbcConnectionAccess access = em.unwrap(SessionImpl.class).getJdbcConnectionAccess();
    Connection con = access.obtainConnection();
    try {
      var dbProduct = con.getMetaData().getClass().getName();
      if (dbProduct.contains("microsoft")) {
        con.createStatement().execute("SET IDENTITY_INSERT " + tableNameOf(entity) + " ON");
      }

      con.setAutoCommit(false);
      var stmt = loadRows(entity, rows, con);
      var inserted = stmt.executeBatch();
      con.commit();

      if (dbProduct.contains("postgres")) {
        String moveAutoIncrement = "ALTER SEQUENCE " + tableNameOf(entity) + "_id_seq RESTART "
            + "WITH " + (inserted.length + 1) + ";";
        con.createStatement().executeUpdate(moveAutoIncrement);
      }
      if (dbProduct.contains("microsoft")) {
        con.createStatement().execute("SET IDENTITY_INSERT " + tableNameOf(entity) + " OFF");
      }
    } finally {
      access.releaseConnection(con);
      em.close();
    }
  }

  private PreparedStatement loadRows(IEntityClass entity, Iterator<Row> rows, Connection con) throws SQLException {
    AtomicInteger rCount = new AtomicInteger(0);
    List<? extends IEntityClassField> fields = entity.getFields();
    var query = buildInsertQuery(entity, fields);
    LOGGER.info("Prepared insert query: " + query);
    var stmt = con.prepareStatement(query, Statement.NO_GENERATED_KEYS);
    rows.forEachRemaining(row -> {
      try {
        rCount.incrementAndGet();
        insertCallValuesAsParameter(fields, rCount, row, stmt);
        stmt.addBatch();
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
    });
    LOGGER.info("Generated " + rCount + " inserts");
    return stmt;
  }

  private static void insertCallValuesAsParameter(List<? extends IEntityClassField> fields, AtomicInteger rCount,
      Row row, PreparedStatement stmt) throws SQLException {
    int c = 0;
    for (var field : fields) {
      Object value;
      if ("id".equals(field.getName())) {
        value = rCount.intValue();
      } else {
        Cell cell = row.getCell(c - 1); // id field does not exists in excel file
        value = getValue(cell);
      }
      c++;
      stmt.setObject(c, value);
    }
  }

  private static String buildInsertQuery(IEntityClass entity, List<? extends IEntityClassField> fields) {
    String colNames = fields.stream().map(IEntityClassField::getName)
        .collect(Collectors.joining(","));
    String tableName = tableNameOf(entity);
    var query = new StringBuilder("INSERT INTO " + tableName + " (" + colNames + ")\nVALUES (");
    var params = fields.stream().map(IEntityClassField::getName)
        .map(f -> "?").collect(Collectors.joining(", "));
    query.append(params);
    query.append(")");
    return query.toString();
  }

  private static String tableNameOf(IEntityClass entity) {
    String tableName = entity.getDatabaseTableName();
    if (StringUtils.isBlank(tableName)) {
      tableName = entity.getSimpleName();
    }
    return tableName;
  }

  public Class<?> createTable(IEntityClass entity) {
    entity.buildJavaSource();
    var java = IJavaConfigurationManager.instance().getJavaConfiguration(entity.getResource().getProject());
    var ivy = IIvyProjectManager.instance().getIvyProject(entity.getResource().getProject());
    Class<?> entityClass;
    try {
      ivy.build(null);
      entityClass = java.getClassLoader().loadClass(entity.getName());
    } catch (Exception ex) {
      throw new RuntimeException("Failed to load entity class " + entity, ex);
    }
    manager.findAll(entityClass); // creates the schema through 'hibernate.hbm2ddl.auto=create'
    return entityClass;
  }

  private static Object getValue(Cell cell) {
    if (cell == null) {
      return null;
    }
    if (cell.getCellType() == CellType.NUMERIC) {
      if (DateUtil.isCellDateFormatted(cell)) {
        return new Date(cell.getDateCellValue().getTime());
      }
      return cell.getNumericCellValue();
    } else if (cell.getCellType() == CellType.BOOLEAN) {
      return cell.getBooleanCellValue();
    }
    return cell.getStringCellValue();
  }

}
