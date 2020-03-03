package com.neverpile.common.authorization.basic;

import java.util.Set;

import com.neverpile.common.authorization.api.Action;
import com.neverpile.common.authorization.api.AuthorizationContext;
import com.neverpile.common.authorization.api.AuthorizationService;

/**
 * A trivial implementation of {@link AuthorizationService} which allows all access attempts.
 */
public class AllowAllAuthorizationService implements AuthorizationService {
  @Override
  public boolean isAccessAllowed(final String resourceSpecifier, final Set<Action> actions,
      final AuthorizationContext context) {
    return true;
  }
}
