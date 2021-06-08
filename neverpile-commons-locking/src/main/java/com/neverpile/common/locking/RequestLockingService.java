package com.neverpile.common.locking;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import com.neverpile.common.locking.LockService.LockRequestResult;

@Service
@Scope(
    scopeName = WebApplicationContext.SCOPE_REQUEST,
    proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestLockingService {
  public enum Mode {
    /**
     * Resources do not actively participate in locking. Clients may perform cooperative locking
     * using a {@link LockService} or other external means, but resources only preven lost updates
     * using optimistic locking/versioning.
     */
    OPTIMISTIC,
    /**
     * Mutating calls to resources require a lock token via the
     * <code>X-Neverpile-Lock-Token</code>-header, otherwise they are rejected.
     */
    EXPLICIT,
    /**
     * Mutating calls to resources without a lock token will cause resources to actively acquire a
     * lock token on the client's behalf.
     */
    IMPLICIT
  }

  public interface RequestScopedLock {
    void releaseIfLocked();
  }

  @Autowired(
      required = false)
  private LockService lockService;

  @Autowired
  private HttpServletRequest request;

  public RequestScopedLock performLocking(final String scopeId, final Mode mode) {
    String lockToken = request.getHeader(LockService.LOCK_TOKEN_HEADER);

    LockRequestResult result = null;
    switch (mode){
      case EXPLICIT :
        if (null == lockService) {
          throw new UnsupportedOperationException(
              "Resource is configured for explicit locking but no lock service is available");
        }
        if (null == lockToken) {
          throw new LockedException("Required " + LockService.LOCK_TOKEN_HEADER + " header is missing");
        }
        if (!lockService.verifyLock(scopeId, lockToken)) {
          throw new LockedException("Lock token is invalid or expired");
        }
        break;

      case IMPLICIT :
        if (null == lockService) {
          throw new UnsupportedOperationException(
              "Resource is configured for implicit locking but no lock service is available");
        }
        if (null != lockToken) {
          // if a lock token is provided it must be valid
          if (!lockService.verifyLock(scopeId, lockToken)) {
            throw new LockedException("Lock token is invalid or expired");
          }
        } else {
          Principal userPrincipal = request.getUserPrincipal();
          result = lockService.tryAcquireLock(scopeId, userPrincipal != null ? userPrincipal.getName() : "anonymous");
          if (!result.isSuccess()) {
            throw new LockedException("Failed to implicitly lock a resource");
          }
        }
        break;

      default :
      case OPTIMISTIC :
        // nothing to do
        break;
    }

    final String tokenToUnlock = null != result && result.isSuccess() ? result.getToken() : null;
    return () -> {
      if (null != tokenToUnlock) {
        lockService.releaseLock(scopeId, tokenToUnlock);
      }
    };
  }
}
