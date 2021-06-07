package com.neverpile.common.locking.jpa;

import java.time.Instant;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * An entity representing a lock's state.
 */
@Entity
@Table(
    name = "locks")
public class LockStateEntity {
  @Id
  private String scope;

  private String ownerId;

  private Instant validUntil;

  private String lockToken;

  public String getScope() {
    return scope;
  }

  public void setScope(String lockTarget) {
    this.scope = lockTarget;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
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
