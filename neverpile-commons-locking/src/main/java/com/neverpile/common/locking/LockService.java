package com.neverpile.common.locking;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public interface LockService {
  public class LockState {
    private String ownerId;
    private String ownerName;
    private Instant validUntil;

    public LockState() {
    }

    public LockState(String ownerId, String ownerName, Instant validUntil) {
      super();
      this.ownerId = ownerId;
      this.ownerName = ownerName;
      this.validUntil = validUntil;
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

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
      result = prime * result + ((ownerName == null) ? 0 : ownerName.hashCode());
      result = prime * result + ((validUntil == null) ? 0 : validUntil.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      LockState other = (LockState) obj;
      return Objects.equals(other.ownerId, ownerId) && Objects.equals(other.ownerName, ownerName)
          && Objects.equals(other.validUntil, validUntil);
    }
  }

  public class LockRequestResult {
    private boolean success;
    private String token;
    private LockState state;

    public LockRequestResult() {
    }
    
    public LockRequestResult(boolean success, String token, LockState state) {
      super();
      this.success = success;
      this.token = token;
      this.state = state;
    }

    public boolean isSuccess() {
      return success;
    }

    public void setSuccess(boolean success) {
      this.success = success;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public LockState getState() {
      return state;
    }

    public void setState(LockState state) {
      this.state = state;
    }
  }

  public class LockLostException extends Exception {
    private static final long serialVersionUID = 1L;
  }

  LockRequestResult tryAcquireLock(String lockTarget, String ownerId, String ownerName);

  LockState extendLock(String lockTarget, String token) throws LockLostException;

  void releaseLock(String lockTarget, String token);

  Optional<LockState> queryLock(String lockTarget);
}
