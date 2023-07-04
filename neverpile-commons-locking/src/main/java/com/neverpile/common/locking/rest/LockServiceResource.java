package com.neverpile.common.locking.rest;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
import com.neverpile.common.locking.LockService.LockLostException;
import com.neverpile.common.locking.LockService.LockRequestResult;
import com.neverpile.common.locking.LockService.LockState;

import io.micrometer.core.annotation.Timed;

/**
 * A REST resource for managing locks.
 */
@RestController
@ConditionalOnWebApplication
@RequestMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnBean(LockService.class)
public class LockServiceResource {
  public static final String PREFIX = "/api/v1/locks";

  @Autowired
  private LockService lockService;

  @GetMapping(PREFIX + "/{scope}")
  @Timed(description = "get lock status", value = "fusion.lock.get")
  public LockState queryLock(@PathVariable("scope") final String scope) throws NotFoundException {
    return lockService.queryLock(scope).orElseThrow(NotFoundException::new);
  }

  @PostMapping(PREFIX + "/{scope}")
  @Timed(description = "try to acquire a lock", value = "fusion.lock.acquire")
  public LockRequestResult tryAcquireLock(@PathVariable("scope") final String scope, final Principal principal,
      @RequestParam(name = "ownerId", required = false) String ownerId) throws ConflictException {
    // derive owner id and name from principal if not explicitly set
    if (null == ownerId && null != principal)
      ownerId = principal.getName();

    LockRequestResult result = lockService.tryAcquireLock(scope, ownerId);
    if (!result.isSuccess()) {
      throw new ConflictException();
    }

    return result;
  }

  @PutMapping(PREFIX + "/{scope}")
  @Timed(description = "extend a lock", value = "fusion.lock.extend")
  public LockState extendLock(@PathVariable("scope") final String scope, @RequestParam("token") String token,
      final Principal principal, @RequestParam(name = "ownerId", required = false) String ownerId)
      throws LockLostException {
    // derive owner id and name from principal if not explicitly set
    if (null == ownerId && null != principal)
      ownerId = principal.getName();

    return lockService.extendLock(scope, token, ownerId);
  }

  @DeleteMapping(PREFIX + "/{scope}")
  @Timed(description = "release a lock", value = "fusion.lock.release")
  public ResponseEntity<?> release(@PathVariable("scope") final String scope, @RequestParam("token") String token) {
    lockService.releaseLock(scope, token);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/api-noauth/v1/locks/{scope}/release")
  @Timed(description = "release a lock", value = "fusion.lock.release")
  public ResponseEntity<?> releaseNoauth(@PathVariable("scope") final String scope,
      @RequestParam("token") String token) {
    lockService.releaseLock(scope, token);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(PREFIX + "/{scope}")
  @Timed(description = "try to contest a lock", value = "fusion.lock.contest")
  public ResponseEntity<?> contestLock(@PathVariable("scope") final String scope, final Principal principal,
      @RequestParam(name = "contestantId", required = false) String contestantId) throws NoContestException {
    // derive contestant id and name from principal if not explicitly set
    if (null == contestantId) {
      if (null != principal) {
        contestantId = principal.getName();
      } else {
        contestantId = "unknown";
      }
    }

    boolean result = lockService.contestLock(scope, contestantId);
    if (!result) {
      throw new NoContestException();
    }

    return ResponseEntity.ok().build();
  }
}
