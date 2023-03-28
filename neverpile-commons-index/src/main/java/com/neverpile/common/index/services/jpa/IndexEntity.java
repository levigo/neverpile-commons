package com.neverpile.common.index.services.jpa;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Index;
import javax.persistence.Table;

import org.slf4j.LoggerFactory;

@Entity
@Table(name = "neverpile_index", indexes = { //
    @Index(name = "text_value_index", columnList = "indexPath, textValue"), //
    @Index(name = "datetime_value_index", columnList = "indexPath, dateTimeValue"), //
    @Index(name = "numeric_value_index", columnList = "indexPath, numericValue"),
    @Index(name = "boolean_value_index", columnList = "indexPath, booleanValue")
})
@IdClass(IdAndIndexPath.class)
public class IndexEntity {

  @Id
  private String objectId;

  @Id
  private String indexPath;

  private String textValue;

  private Instant dateTimeValue;

  private BigDecimal numericValue;

  private Boolean booleanValue;


  public IndexEntity() {
  }

  public IndexEntity(String objectId, String indexPath, String value) {
    this.objectId = objectId;
    this.indexPath = indexPath;
    populateValue(objectId, indexPath, value);
  }

  protected void populateValue(String objectId, String indexPath, String value) {
    try {
      setDateTimeValue(Instant.parse(value));
      return;
    } catch (DateTimeParseException e1) {
      // not a time -> try next
    }
    try {
      BigDecimal bigDecimal = new BigDecimal(value);
      // if the number is too big it will be saved as string.
      if (bigDecimal.precision() > 19) {
        LoggerFactory.getLogger(IndexEntity.class).warn("Decimal value is too long:\n" //
            + "{\n\tobjectId: " + objectId //
            + "\n\tindexPath: " + indexPath //
            + "\n\tvalue: " + value + "\n}\n" //
        );
      } else {
        setNumericValue(bigDecimal);
        return;
      }
    } catch (NumberFormatException e2) {
      // not a number -> try next
    }
    if (value.length() > 254) {
      LoggerFactory.getLogger(IndexEntity.class).warn("String value is too long:\n" //
          + "{\n\tobjectId: " + objectId //
          + "\n\tindexPath: " + indexPath //
          + "\n\tvalue: " + value + "\n}\n" //
      );
      setTextValue(value.substring(0, 254));
    } else {
      setTextValue(value);
    }

  }

  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  public String getIndexPath() {
    return indexPath;
  }

  public void setIndexPath(String indexPath) {
    this.indexPath = indexPath;
  }

  public String getTextValue() {
    return textValue;
  }

  public void setTextValue(String textValue) {
    this.textValue = textValue;
  }

  public Instant getDateTimeValue() {
    return dateTimeValue;
  }

  public void setDateTimeValue(Instant dateTimeValue) {
    dateTimeValue = dateTimeValue;
  }

  public BigDecimal getNumericValue() {
    return numericValue;
  }

  public void setNumericValue(BigDecimal numericValue) {
    this.numericValue = numericValue;
  }

  public Boolean getBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(Boolean booleanValue) {
    this.booleanValue = booleanValue;
  }

  @Override
  public int hashCode() {
    return Objects.hash(objectId, indexPath, dateTimeValue, numericValue, textValue, booleanValue);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    IndexEntity other = (IndexEntity) obj;
    return Objects.equals(objectId, other.objectId) //
        && Objects.equals(indexPath, other.indexPath) //
        && Objects.equals(dateTimeValue, other.dateTimeValue) //
        && Objects.equals(numericValue, other.numericValue) //
        && Objects.equals(textValue, other.textValue) //
        && Objects.equals(booleanValue, other.booleanValue);
  }

  @Override
  public String toString() {
    return "{\n\tobjectId: " + this.getObjectId() //
        + "\n\tindexPath: " + this.getIndexPath() //
        + "\n\tvalue: " + //
        (this.getTextValue() != null ? "(String): " + this.getTextValue() :  //
            this.getNumericValue() != null ? "(Numeric): " + this.getNumericValue() : //
                this.getDateTimeValue() != null ? "(DateTime): " + this.getDateTimeValue() : //
                  this.getBooleanValue() != null ? "(Boolean): " + this.getBooleanValue() : //
                    "null") //
        + "\n}\n"; //
  }

  public void updateValue(IndexEntity indexEntity) {
    this.textValue = indexEntity.textValue;
    this.dateTimeValue = indexEntity.dateTimeValue;
    this.numericValue = indexEntity.numericValue;
    this.booleanValue = indexEntity.booleanValue;
  }
}

