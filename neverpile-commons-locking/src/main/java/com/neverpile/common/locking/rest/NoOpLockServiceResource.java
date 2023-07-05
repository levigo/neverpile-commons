package com.neverpile.common.locking.rest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neverpile.common.locking.LockService;
import com.neverpile.common.locking.LockService.LockRequestResult;
import com.neverpile.common.locking.LockService.LockState;

import io.micrometer.core.annotation.Timed;

/**
 * A fallback lock-resource for cases where locking is disabled. It supplies frontend applications
 * with always successful lock operations.
 */
@RestController
@ConditionalOnWebApplication
@RequestMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnMissingBean(LockService.class)
public class NoOpLockServiceResource {
  public static final String PREFIX = "/api/v1/locks";

  @GetMapping(PREFIX + "/{scope}")
  @Timed(description = "get lock status", value = "fusion.lock.get")
  public LockState queryLock(@PathVariable("scope") final String scope) {
    LockState fakeLock = new LockState();
    fakeLock.setOwnerId("fake");
    fakeLock.setValidUntil(Instant.now().plus(30, ChronoUnit.DAYS));
    return fakeLock;
  }

  @PostMapping(PREFIX + "/{scope}")
  @Timed(description = "try to acquire a lock", value = "fusion.lock.acquire")
  public LockRequestResult tryAcquireLock(@PathVariable("scope") final String scope,
      @RequestParam(name = "ownerId", required = false) String ownerId) {
    LockState fakeLock = new LockState();
    fakeLock.setOwnerId(ownerId);
    fakeLock.setValidUntil(Instant.now().plus(30, ChronoUnit.DAYS));

    return new LockRequestResult(true, "fake", fakeLock);
  }

  @PutMapping(PREFIX + "/{scope}")
  @Timed(description = "extend a lock", value = "fusion.lock.extend")
  public LockState extendLock(@PathVariable("scope") final String scope,
      @RequestParam(name = "ownerId", required = false) String ownerId) {
    LockState fakeLock = new LockState();
    fakeLock.setOwnerId(ownerId);
    fakeLock.setValidUntil(Instant.now().plus(30, ChronoUnit.DAYS));
    return fakeLock;
  }

  @DeleteMapping(PREFIX + "/{scope}")
  @Timed(description = "release a lock", value = "fusion.lock.release")
  public ResponseEntity<?> release(@PathVariable("scope") final String scope) {
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/api-noauth/v1/locks/{scope}/release")
  @Timed(description = "release a lock", value = "fusion.lock.release")
  public ResponseEntity<?> releaseNoauth(@PathVariable("scope") final String scope) {
    return ResponseEntity.noContent().build();
  }

  @PostMapping(PREFIX + "/{scope}/contest")
  @Timed(description = "try to contest a lock", value = "fusion.lock.contest")
  public ResponseEntity<?> contestLock(@PathVariable("scope") final String scope,
      @RequestParam(name = "contestantId", required = false) String contestantId) {
    return ResponseEntity.ok().build();
  }

  @DeleteMapping(PREFIX + "/{scope}/contest")
  @Timed(description = "resolve a lock contest", value = "fusion.lock.contest.resolve")
  public ResponseEntity<?> resolveContest(@PathVariable("scope") final String scope,
      @RequestParam("token") String token) {
    return ResponseEntity.ok().build();
  }
}