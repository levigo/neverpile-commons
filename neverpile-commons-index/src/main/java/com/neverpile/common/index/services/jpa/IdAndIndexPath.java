package com.neverpile.common.index.services.jpa;

import java.io.Serializable;

public class IdAndIndexPath implements Serializable {
  private static final long serialVersionUID = 1L;

  private String collectionId;

  private String metadataPath;

  public String getObjectId() {
    return collectionId;
  }

  public void setObjectId(final String id) {
    this.collectionId = id;
  }

  public String getIndexPath() {
    return metadataPath;
  }

  public void setIndexPath(final String metadataPath) {
    this.metadataPath = metadataPath;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((collectionId == null) ? 0 : collectionId.hashCode());
    result = prime * result + ((metadataPath == null) ? 0 : metadataPath.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    IdAndIndexPath other = (IdAndIndexPath) obj;
    if (collectionId == null) {
      if (other.collectionId != null)
        return false;
    } else if (!collectionId.equals(other.collectionId))
      return false;
    if (metadataPath == null) {
      return other.metadataPath == null;
    } else
      return metadataPath.equals(other.metadataPath);
  }
}
