package com.neverpile.common.authorization.policy.impl;

import java.util.List;

import org.springframework.security.core.Authentication;

public interface AuthenticationMatcher {

  boolean matchAuthentication(Authentication authentication, List<String> subjects);

}
