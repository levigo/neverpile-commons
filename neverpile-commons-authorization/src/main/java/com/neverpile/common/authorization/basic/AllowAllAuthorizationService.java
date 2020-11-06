package com.neverpile.common.authorization.basic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.neverpile.common.authorization.api.Action;
import com.neverpile.common.authorization.api.AuthorizationContext;
import com.neverpile.common.authorization.api.AuthorizationService;
import com.neverpile.common.authorization.api.Permission;
import com.neverpile.common.authorization.policy.Effect;

/**
 * A trivial implementation of {@link AuthorizationService} which allows all access attempts.
 */
public class AllowAllAuthorizationService implements AuthorizationService {
  @Override
  public boolean isAccessAllowed(final String resourceSpecifier, final Set<Action> actions,
      final AuthorizationContext context) {
    return true;
  }

  @Override
  public List<Permission> getPermissions(final String resourceSpecifier, final AuthorizationContext context) {
    return Collections.singletonList(new Permission(Effect.ALLOW, Arrays.asList("*")));
  }
}
