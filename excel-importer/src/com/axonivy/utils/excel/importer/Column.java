package com.axonivy.utils.excel.importer;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Column {

  private String name;
  private Class<?> type;
  private Integer databaseFieldLength;

  public Column(String name, Class<?> type, Integer databaseFieldLength) {
    super();
    this.name = name;
    this.type = type;
    this.databaseFieldLength = databaseFieldLength;
  }

  public Column(String name, Class<?> type) {
    super();
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Class<?> getType() {
    return type;
  }

  public void setType(Class<?> type) {
    this.type = type;
  }

  public Integer getDatabaseFieldLength() {
    return databaseFieldLength;
  }

  public void setDatabaseFieldLength(Integer databaseFieldLength) {
    this.databaseFieldLength = databaseFieldLength;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    Column column = (Column) object;

    return new EqualsBuilder()
        .append(name, column.name)
        .append(type, column.type)
        .append(databaseFieldLength, column.databaseFieldLength)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(name)
        .append(type)
        .append(databaseFieldLength)
        .toHashCode();
  }

  @Override
  public String toString() {
    return "Column{" + "name='" + name + "\'" + ", type=" + type + ", databaseFieldLength="
        + (databaseFieldLength != null ? databaseFieldLength : "null") + "}";
  }

}