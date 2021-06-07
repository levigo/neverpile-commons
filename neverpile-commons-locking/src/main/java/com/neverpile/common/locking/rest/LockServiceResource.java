package com.neverpile.common.locking.rest;

import java.security.Principal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping(
    path = "/api/v1/locks/{target}",
    produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnBean(LockService.class)
public class LockServiceResource {
  @Autowired
  private LockService lockService;

  @GetMapping
  @Timed(
      description = "get lock status",
      value = "fusion.lock.get")
  public Optional<LockState> queryLock(@PathVariable("target") final String target) {
    return lockService.queryLock(target);
  }

  @PutMapping
  @Timed(
      description = "try to acquire a lock",
      value = "fusion.lock.acquire")
  public LockRequestResult tryAcquireLock(@PathVariable("target") final String target, final Principal principal,
      @RequestParam(
          name = "ownerId",
          required = false) String ownerId,
      @RequestParam(
          name = "ownerName",
          required = false) String ownerName) {
    // derive owner id and name from principal if not explicitly set
    if (null == ownerId && null != principal)
      ownerId = principal.getName();
    if (null == ownerName && null != principal)
      ownerName = principal.toString();

    return lockService.tryAcquireLock(target, ownerId, ownerName);
  }

  @PutMapping
  @Timed(
      description = "extend a lock",
      value = "fusion.lock.extend")
  public LockState extendLock(@PathVariable("target") final String target, @RequestParam("token") String token) throws LockLostException {
    return lockService.extendLock(target, token);
  }

  @DeleteMapping
  @Timed(
      description = "release a lock",
      value = "fusion.lock.release")
  public void release(@PathVariable("target") final String target, @RequestParam("token") String token) {
    lockService.releaseLock(target, token);
  }
}
