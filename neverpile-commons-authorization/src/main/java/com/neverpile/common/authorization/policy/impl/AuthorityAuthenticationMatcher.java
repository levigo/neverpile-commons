package com.neverpile.common.authorization.policy.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthorityAuthenticationMatcher implements AuthenticationMatcher {

  public static final String ROLE = "role:";
  
  @Override
  public boolean matchAuthentication(final Authentication authentication, final List<String> subjects) {
    return authentication.getAuthorities().stream().anyMatch(
        a -> subjects.contains(ROLE + a.getAuthority()));
  }

  @Override
  public List<Hint> getHints() {
    return Arrays.asList(new Hint(ROLE, "a role/granted authority"));
  }
}
