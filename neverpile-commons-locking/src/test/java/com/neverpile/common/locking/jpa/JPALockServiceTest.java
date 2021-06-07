package com.neverpile.common.locking.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.transaction.TestTransaction;

import com.neverpile.common.locking.LockService.LockLostException;
import com.neverpile.common.locking.LockService.LockRequestResult;
import com.neverpile.common.locking.LockService.LockState;
import com.neverpile.common.locking.LockingConfiguration;

@DataJpaTest
@EnableAutoConfiguration
@ContextConfiguration(
    classes = {
        JPALockService.class, LockingConfiguration.class
    })
@TestPropertySource(
    properties = {
        "neverpile.locking.jpa.enabled=true",
        "neverpile.locking.validity-duration=PT5S"
    })
public class JPALockServiceTest {
  @Autowired
  private JPALockService lockService;

  @Autowired
  private LockStateRepository repository;

  @AfterEach
  public void cleanup() {
    repository.deleteAll();
  }

  @Test
  public void testThat_lockCanBeAcquired() {
    Instant start = Instant.now();

    LockRequestResult result = lockService.tryAcquireLock("dummy", "anOwnerId");
    TestTransaction.flagForCommit();
    TestTransaction.end();

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getState().getOwnerId()).isEqualTo("anOwnerId");
    assertThat(result.getState().getValidUntil()).isAfter(start);
    assertThat(result.getState().getValidUntil()).isBefore(Instant.now().plusSeconds(60)); // default
                                                                                           // validity
    assertThat(result.getToken()).isNotEmpty();

    // verify repository contents
    TestTransaction.start();
    LockStateEntity entry = repository.findById("dummy").orElseThrow(() -> new AssertionError("lock not present"));

    assertThat(entry.getOwnerId()).isEqualTo("anOwnerId");
    assertThat(entry.getLockToken()).isEqualTo(result.getToken());
  }

  @Test
  public void testThat_lockCanBeReleased() {
    LockRequestResult result = lockService.tryAcquireLock("dummy", "anOwnerId");
    TestTransaction.flagForCommit();
    TestTransaction.end();

    TestTransaction.start();
    lockService.releaseLock("dummy", result.getToken());
    TestTransaction.flagForCommit();
    TestTransaction.end();

    // verify repository contents
    assertThat(repository.findById("dummy")).isNotPresent();
  }

  @Test
  public void testThat_lockCanBeExtended() throws LockLostException, InterruptedException {
    LockRequestResult result = lockService.tryAcquireLock("dummy", "anOwnerId");
    TestTransaction.flagForCommit();
    TestTransaction.end();

    Thread.sleep(1000);

    TestTransaction.start();
    LockState extended = lockService.extendLock("dummy", result.getToken());
    TestTransaction.flagForCommit();
    TestTransaction.end();

    // verify extension
    assertThat(extended.getValidUntil()).isAfter(result.getState().getValidUntil().plusMillis(500));

    // verify repository contents
    assertThat(repository.findById("dummy").orElseThrow(
        () -> new AssertionError("lock not present")).getValidUntil()).isEqualTo(extended.getValidUntil());
  }

  @Test
  public void testThat_lockCanBeQueried() throws LockLostException, InterruptedException {
    LockRequestResult acquired = lockService.tryAcquireLock("dummy", "anOwnerId");
    TestTransaction.flagForCommit();
    TestTransaction.end();

    Optional<LockState> lock = lockService.queryLock("dummy");
    assertThat(lock).isPresent();
    assertThat(lock.get().getOwnerId()).isEqualTo("anOwnerId");
    assertThat(lock.get().getValidUntil()).isEqualTo(acquired.getState().getValidUntil());
  }

  @Test
  public void testThat_acquisitionFailsOnCollision() {
    lockService.tryAcquireLock("dummy", "anotherOwnerId");
    TestTransaction.flagForCommit();
    TestTransaction.end();

    TestTransaction.start();
    LockRequestResult fail = lockService.tryAcquireLock("dummy", "anOwnerId");
    TestTransaction.end();

    assertThat(fail.isSuccess()).isFalse();
  }

  @Test
  public void testThat_cannotReleaseForeignLock() {
    lockService.tryAcquireLock("dummy", "anotherOwnerId");
    TestTransaction.flagForCommit();
    TestTransaction.end();

    // must have no effect!
    TestTransaction.start();
    lockService.releaseLock("dummy", "whateverToken");
    TestTransaction.flagForCommit();
    TestTransaction.end();

    assertThat(repository.findById("dummy")).isPresent();
  }

  @Test
  public void testThat_cannotExtendForeignLock() {
    lockService.tryAcquireLock("dummy", "anotherOwnerId");
    TestTransaction.flagForCommit();
    TestTransaction.end();

    Assertions.assertThrows(LockLostException.class, () -> lockService.extendLock("dummy", "whateverToken"));
  }

  @Test
  public void testThat_housekeepingCleansOldLocks() throws InterruptedException {
    lockService.tryAcquireLock("dummy1", "anotherOwnerId");
    TestTransaction.flagForCommit();
    TestTransaction.end();

    Thread.sleep(7000);

    TestTransaction.start();
    lockService.tryAcquireLock("dummy2", "anotherOwnerId");
    TestTransaction.flagForCommit();
    TestTransaction.end();

    // dummy1 must be expired
    assertThat(StreamSupport.stream(repository.findAll().spliterator(), false).count()).isEqualTo(1);
  }
}
