package com.neverpile.common.locking;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A service providing explicit/pessimistic/cooperative locking services.
 */
public interface LockService {
  /**
   * A representation of a lock's state.
   */
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

    /**
     * Get the id of the current lock owner.
     * 
     * @return the owner id
     */
    public String getOwnerId() {
      return ownerId;
    }

    /**
     * Set the owner id
     * 
     * @param ownerId the owner id
     */
    public void setOwnerId(String ownerId) {
      this.ownerId = ownerId;
    }

    /**
     * Get the human-readable name of the current lock owner.
     * 
     * @return the owner name
     */
    public String getOwnerName() {
      return ownerName;
    }

    /**
     * Set the human-readable name of the current lock owner.
     * 
     * @param ownerName the owner name
     */
    public void setOwnerName(String ownerName) {
      this.ownerName = ownerName;
    }

    /**
     * Get the end of the validity period of the lock.
     * 
     * @return the end of the validity period
     */
    public Instant getValidUntil() {
      return validUntil;
    }

    /**
     * Set the end of the validity period of the lock.
     * 
     * @param validUntil the end of the validity period
     */
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

  /**
   * A prepresentation of a lock acquisition request.
   */
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

    /**
     * Return whether the request was successful.
     * 
     * @return <code>true</code> if the request was successful
     */
    public boolean isSuccess() {
      return success;
    }

    /**
     * Set whether the request was successful.
     * 
     * @param success Return whether the request was successful
     */
    public void setSuccess(boolean success) {
      this.success = success;
    }

    /**
     * Get the secret lock token. Knowledge of the lock token asserts ownership of the lock.
     * 
     * @return the lock token
     */
    public String getToken() {
      return token;
    }

    /**
     * Set the secret lock token.
     * 
     * @param token the lock token
     */
    public void setToken(String token) {
      this.token = token;
    }

    /**
     * Get the lock state.
     * 
     * @return the lock state
     */
    public LockState getState() {
      return state;
    }

    /**
     * Set the lock state.
     * 
     * @param state the lock state
     */
    public void setState(LockState state) {
      this.state = state;
    }
  }

  /**
   * An exception indicating that a lock extension request failed, because the lock has not been
   * refreshed in time and has been acquired by a third party.
   */
  @ResponseStatus(HttpStatus.GONE)
  public class LockLostException extends Exception {
    private static final long serialVersionUID = 1L;
  }

  /**
   * Try to acquire the lock for the given scope.
   * 
   * @param scope the lock scope
   * @param ownerId the id of the prospective lock owner
   * @param ownerName the human-readable name of the prospective lock owner
   * @return the LockRequestResult
   */
  LockRequestResult tryAcquireLock(String scope, String ownerId, String ownerName);

  /**
   * Extend the validity of a lock.
   * 
   * @param scope the lock scope
   * @param token the secret lock token
   * @return the new LockState
   * @throws LockLostException if the lock extension request failed, because the lock has not been
   *           refreshed in time and this has been acquired by a third party.
   */
  LockState extendLock(String scope, String token) throws LockLostException;

  /**
   * Release a lock.
   * 
   * @param scope the lock scope
   * @param token the secret lock token
   */
  void releaseLock(String scope, String token);

  /**
   * Query the state of a lock.
   * 
   * @param scope the lock scope
   * @return the current lock state or the empty {@link Optional} if the lock does not exist
   */
  Optional<LockState> queryLock(String scope);
}
