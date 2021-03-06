package com.neverpile.common.locking.jpa;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

import com.neverpile.common.locking.LockService;
import com.neverpile.common.locking.LockingConfiguration;

/**
 * A JPA-based implementation of {@link LockService}. To enable it, JPA must be configured in the
 * environment and the property <code>neverpile.locking.jpa.enabled</code> must be set to
 * <code>true</code>.
 * <p>
 * The implementation will contribute the {@link LockStateEntity} to JPA
 */
@Service
@ConditionalOnProperty(
    name = "neverpile.locking.jpa.enabled",
    havingValue = "true")
@ConditionalOnClass(
    name = "javax.persistence.EntityManager")
@ComponentScan
public class JPALockService implements LockService {
  private static final Logger LOGGER = LoggerFactory.getLogger(JPALockService.class);

  @Autowired
  private LockStateRepository lockStateRepository;

  @Autowired
  private LockingConfiguration lockingConfiguration;

  private AtomicLong nextHousekeeping = new AtomicLong();

  @Override
  public LockRequestResult tryAcquireLock(String scope, String ownerId) {
    performHousekeeping();

    return tryAcquire(scope, ownerId, UUID.randomUUID().toString());
  }

  private LockRequestResult tryAcquire(String scope, String ownerId, String token) {
    Optional<LockStateEntity> existing = lockStateRepository.findById(scope);

    // Existing lock owned by requester? Just extend it. Expired? Delete existing lock.
    if (existing.isPresent()) {
      LockStateEntity lse = existing.get();
      if (Objects.equals(lse.getOwnerId(), ownerId) || lse.getValidUntil().isBefore(Instant.now())) {
        lockStateRepository.delete(lse);
      } else {
        return new LockRequestResult(false, null, new LockState(lse.getOwnerId(), lse.getValidUntil()));
      }
    }

    // prepare new lock
    LockState state = new LockState(ownerId, Instant.now().plus(lockingConfiguration.getValidityDuration()));

    LockStateEntity lse = new LockStateEntity();
    lse.setScope(scope);
    lse.setLockToken(token);
    lse.setOwnerId(ownerId);
    lse.setValidUntil(state.getValidUntil());

    // persist lock
    try {
      lockStateRepository.save(lse);
    } catch (EntityExistsException e) {
      // late lock collision - report as fail
      LockRequestResult failure = new LockRequestResult(false, null,
          new LockState(lse.getOwnerId(), lse.getValidUntil()));

      // still try to tell the requester who owns the lock
      try {
        lockStateRepository.findById(scope).ifPresent(
            existingLse -> failure.setState(new LockState(existingLse.getOwnerId(), existingLse.getValidUntil())));
      } catch (PersistenceException f) {
        // ignore exceptions during this phase
        LOGGER.warn("Can't report actual owner on late collision", f);
      }

      return failure;
    }

    return new LockRequestResult(true, token, state);
  }

  @Override
  public LockState extendLock(String scope, String token, String ownerId) throws LockLostException {
    Optional<LockStateEntity> existing = lockStateRepository.findById(scope);

    if (existing.isPresent()) {
      LockStateEntity lse = existing.get();
      if (lse.getValidUntil().isAfter(Instant.now())) {
        if (Objects.equals(lse.getLockToken(), token)) {
          lse.setValidUntil(Instant.now().plus(lockingConfiguration.getValidityDuration()));

          // persist updated lock
          try {
            lockStateRepository.save(lse);

            return new LockState(lse.getOwnerId(), lse.getValidUntil());
          } catch (EntityExistsException e) {
            // late lock collision - report as fail (fall out)
          }
        } else {
          // has been acquired otherwise
          throw new LockLostException();
        }
      }
    }

    // try silent re-acquire
    LockRequestResult result = tryAcquire(scope, ownerId, token);
    if (!result.isSuccess()) {
      // re-acquire failed
      throw new LockLostException();
    }

    return result.getState();
  }

  @Override
  public void releaseLock(String scope, String token) {
    Optional<LockStateEntity> existing = lockStateRepository.findById(scope);
    if (existing.isPresent()) {
      LockStateEntity lse = existing.get();
      if (Objects.equals(lse.getLockToken(), token)) {
        lockStateRepository.delete(lse);
      }
    }

    // in all other cases just ignore the request
  }

  @Override
  public Optional<LockState> queryLock(String scope) {
    return lockStateRepository.findById(scope).map(
        lse -> new LockService.LockState(lse.getOwnerId(), lse.getValidUntil()));
  }

  /**
   * Expire locks older that one lock validity duration
   */
  private void performHousekeeping() {
    long nextRun = nextHousekeeping.get();
    Instant now = Instant.now();
    if (nextRun < now.toEpochMilli() && nextHousekeeping.compareAndSet(nextRun,
        now.plus(lockingConfiguration.getValidityDuration()).toEpochMilli())) {
      // small grace period
      Instant deleteBefore = now.minusSeconds(1);
      LOGGER.debug("Running housekeeping, deleting locks expired before {}", deleteBefore);
      lockStateRepository.deleteByValidUntilBefore(deleteBefore);
    }
  }

  @Override
  public boolean verifyLock(String scope, String token) {
    return lockStateRepository.findById(scope).map(lse ->
        // matching token?
        lse.getLockToken().equals(token) ||
        // expired lock state means "not locked"
        Instant.now().isAfter(lse.getValidUntil()))
      // not found -> ok as well.
      .orElse(true);
  }
}
