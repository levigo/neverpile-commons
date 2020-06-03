package com.neverpile.common.authorization.policy.impl;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.neverpile.common.authorization.policy.AccessRule;

@Component
public class AuthorityAuthenticationMatcher implements AuthenticationMatcher {

  @Override
  public boolean matchAuthentication(final Authentication authentication, final List<String> subjects) {
    return authentication.getAuthorities().stream().anyMatch(
        a -> subjects.contains(AccessRule.ROLE + a.getAuthority()));
  }

}
