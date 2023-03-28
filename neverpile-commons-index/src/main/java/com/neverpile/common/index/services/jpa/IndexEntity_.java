package com.neverpile.fusion.jpa.collection.index;

import java.math.BigDecimal;
import java.time.Instant;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.neverpile.common.index.services.jpa.IndexEntity;

@StaticMetamodel(IndexEntity.class)
public class IndexEntity_ {
  public static volatile SingularAttribute<IndexEntity, String> objectId;
  public static volatile SingularAttribute<IndexEntity, String> indexPath;
  public static volatile SingularAttribute<IndexEntity, String> IndexTextValue;
  public static volatile SingularAttribute<IndexEntity, Instant> IndexDateTimeValue;
  public static volatile SingularAttribute<IndexEntity, BigDecimal> IndexNumericValue;
  public static volatile SingularAttribute<IndexEntity, Boolean> IndexBooleanValue;
}
