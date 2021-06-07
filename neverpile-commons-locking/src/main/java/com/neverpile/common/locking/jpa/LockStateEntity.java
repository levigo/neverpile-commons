package com.neverpile.common.locking.jpa;

import java.time.Instant;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "locks")
public class LockStateEntity {
  @Id
  private String lockTarget;
  
  private String ownerId;
  
  private String ownerName;
  
  private Instant validUntil;
  
  private String lockToken;
  
  public String getLockTarget() {
    return lockTarget;
  }

  public void setLockTarget(String lockTarget) {
    this.lockTarget = lockTarget;
  }
  
  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  public Instant getValidUntil() {
    return validUntil;
  }

  public void setValidUntil(Instant validUntil) {
    this.validUntil = validUntil;
  }

  public String getLockToken() {
    return lockToken;
  }

  public void setLockToken(String lockId) {
    this.lockToken = lockId;
  }
}
