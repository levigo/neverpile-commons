package com.neverpile.common.authorization.policy.impl;

import com.neverpile.common.authorization.api.AuthorizationContext;
import com.neverpile.common.specifier.Specifier;

/**
 * An implementation of {@link AuthorizationContext} which resolves nothing.
 */
public class EmptyAuthorizationContext implements AuthorizationContext {
  @Override
  public Object resolveValue(final Specifier key) {
    return null;
  }
}
