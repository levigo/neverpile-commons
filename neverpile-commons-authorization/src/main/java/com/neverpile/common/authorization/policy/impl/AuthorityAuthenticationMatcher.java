package com.neverpile.common.authorization.policy.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class AuthorityAuthenticationMatcher implements AuthenticationMatcher {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthorityAuthenticationMatcher.class);

  public static final String ROLE = "role:";

  @Override
  public boolean matchAuthentication(final Authentication authentication, final List<String> subjects) {
    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    boolean anyMatch = authorities.stream().anyMatch(a -> subjects.contains(ROLE + a.getAuthority()));

    LOGGER.debug("  The principal's authorities {} {} the subjects {}", authorities,
        anyMatch ? "MATCH" : "do not match", subjects);

    return anyMatch;
  }

  @Override
  public List<Hint> getHints() {
    return Arrays.asList(new Hint(ROLE, "a role/granted authority"));
  }
}
