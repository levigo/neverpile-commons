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

@Service
@ConditionalOnClass(
    name = "javax.persistence.EntityManager")
@ConditionalOnProperty(
    name = "neverpile.locking.jpa.enabled",
    havingValue = "true")
@ComponentScan
public class JPALockService implements LockService {
  private static final Logger LOGGER = LoggerFactory.getLogger(JPALockService.class);

  @Autowired
  private LockStateRepository lockStateRepository;

  @Autowired
  private LockingConfiguration lockingConfiguration;

  private AtomicLong nextHousekeeping = new AtomicLong();

  @Override
  public LockRequestResult tryAcquireLock(String lockTarget, String ownerId, String ownerName) {
    performHousekeeping();

    Optional<LockStateEntity> existing = lockStateRepository.findById(lockTarget);

    // Existing lock owned by requester? Just extend it. Expired? Delete existing lock.
    if (existing.isPresent()) {
      LockStateEntity lse = existing.get();
      if (Objects.equals(lse.getOwnerId(), ownerId) || lse.getValidUntil().isBefore(Instant.now())) {
        lockStateRepository.delete(lse);
      } else {
        return new LockRequestResult(false, null,
            new LockState(lse.getOwnerId(), lse.getOwnerName(), lse.getValidUntil()));
      }
    }

    // prepare new lock
    String token = UUID.randomUUID().toString();
    LockState state = new LockState(ownerId, ownerName, Instant.now().plus(lockingConfiguration.getLockValidity()));

    LockStateEntity lse = new LockStateEntity();
    lse.setLockTarget(lockTarget);
    lse.setLockToken(token);
    lse.setOwnerId(ownerId);
    lse.setOwnerName(ownerName);
    lse.setValidUntil(state.getValidUntil());

    // persist lock
    try {
      lockStateRepository.save(lse);
    } catch (EntityExistsException e) {
      // late lock collision - report as fail
      LockRequestResult failure = new LockRequestResult(false, null,
          new LockState(lse.getOwnerId(), lse.getOwnerName(), lse.getValidUntil()));

      // still try to tell the requester who owns the lock
      try {
        lockStateRepository.findById(lockTarget).ifPresent(existingLse -> failure.setState(
            new LockState(existingLse.getOwnerId(), existingLse.getOwnerName(), existingLse.getValidUntil())));
      } catch (PersistenceException f) {
        // ignore exceptions during this phase
        LOGGER.warn("Can't report actual owner on late collision", f);
      }

      return failure;
    }

    return new LockRequestResult(true, token, state);
  }

  @Override
  public LockState extendLock(String lockTarget, String token) throws LockLostException {
    Optional<LockStateEntity> existing = lockStateRepository.findById(lockTarget);

    if (existing.isPresent()) {
      LockStateEntity lse = existing.get();
      if (Objects.equals(lse.getLockToken(), token)) {
        lse.setValidUntil(Instant.now().plus(lockingConfiguration.getLockValidity()));

        // persist updated lock
        try {
          lockStateRepository.save(lse);

          return new LockState(lse.getOwnerId(), lse.getOwnerName(), lse.getValidUntil());
        } catch (EntityExistsException e) {
          // late lock collision - report as fail (fall out)
        }
      }
    }

    throw new LockLostException();
  }

  @Override
  public void releaseLock(String lockTarget, String token) {
    Optional<LockStateEntity> existing = lockStateRepository.findById(lockTarget);
    if (existing.isPresent()) {
      LockStateEntity lse = existing.get();
      if (Objects.equals(lse.getLockToken(), token)) {
        lockStateRepository.delete(lse);
      }
    }

    // in all other cases just ignore the request
  }

  @Override
  public Optional<LockState> queryLock(String lockTarget) {
    return lockStateRepository.findById(lockTarget).map(
        lse -> new LockService.LockState(lse.getOwnerId(), lse.getOwnerName(), lse.getValidUntil()));
  }

  /**
   * Expire locks older that one lock validity duration
   */
  private void performHousekeeping() {
    long nextRun = nextHousekeeping.get();
    Instant now = Instant.now();
    if (nextRun < now.toEpochMilli()
        && nextHousekeeping.compareAndSet(nextRun, now.plus(lockingConfiguration.getLockValidity()).toEpochMilli())) {
      lockStateRepository.deleteByValidUntilBefore(now.minus(lockingConfiguration.getLockValidity())
          // small grace period
          .minusSeconds(10));
    }
  }
}
