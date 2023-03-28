package com.neverpile.common.index.services.jpa;

import java.io.Serializable;

public class IdAndIndexPath implements Serializable {
  private static final long serialVersionUID = 1L;

  private String objectId;

  private String indexPath;

  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(final String id) {
    this.objectId = id;
  }

  public String getIndexPath() {
    return indexPath;
  }

  public void setIndexPath(final String metadataPath) {
    this.indexPath = metadataPath;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
    result = prime * result + ((indexPath == null) ? 0 : indexPath.hashCode());
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
    if (objectId == null) {
      if (other.objectId != null)
        return false;
    } else if (!objectId.equals(other.objectId))
      return false;
    if (indexPath == null) {
      return other.indexPath == null;
    } else
      return indexPath.equals(other.indexPath);
  }
}
