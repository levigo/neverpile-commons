package com.neverpile.common.locking.rest;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
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
@RequestMapping(
    path = "/api/v1/locks",
    produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnBean(LockService.class)
public class LockServiceResource {
  @Autowired
  private LockService lockService;

  @GetMapping("{scope}")
  @Timed(
      description = "get lock status",
      value = "fusion.lock.get")
  public LockState queryLock(@PathVariable("scope") final String scope) throws NotFoundException {
    return lockService.queryLock(scope).orElseThrow(() -> new NotFoundException());
  }

  @PostMapping("{scope}")
  @Timed(
      description = "try to acquire a lock",
      value = "fusion.lock.acquire")
  public LockRequestResult tryAcquireLock(@PathVariable("scope") final String scope, final Principal principal,
      @RequestParam(
          name = "ownerId",
          required = false) String ownerId)
      throws ConflictException {
    // derive owner id and name from principal if not explicitly set
    if (null == ownerId && null != principal)
      ownerId = principal.getName();

    LockRequestResult result = lockService.tryAcquireLock(scope, ownerId);
    if (!result.isSuccess()) {
      throw new ConflictException();
    }

    return result;
  }

  @PutMapping("{scope}")
  @Timed(
      description = "extend a lock",
      value = "fusion.lock.extend")
  public LockState extendLock(@PathVariable("scope") final String scope, @RequestParam("token") String token,
      final Principal principal, @RequestParam(
          name = "ownerId",
          required = false) String ownerId)
      throws LockLostException {
    // derive owner id and name from principal if not explicitly set
    if (null == ownerId && null != principal)
      ownerId = principal.getName();

    return lockService.extendLock(scope, token, ownerId);
  }

  @DeleteMapping("{scope}")
  @Timed(
      description = "release a lock",
      value = "fusion.lock.release")
  public void release(@PathVariable("scope") final String scope, @RequestParam("token") String token) {
    lockService.releaseLock(scope, token);
  }
}
