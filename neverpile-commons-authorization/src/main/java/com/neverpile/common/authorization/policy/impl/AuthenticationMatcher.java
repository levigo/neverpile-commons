package com.neverpile.common.authorization.policy.impl;

import java.util.List;

import org.springframework.security.core.Authentication;

import com.neverpile.common.authorization.api.HintRegistrations;
import com.neverpile.common.authorization.policy.SubjectHints;

@SubjectHints
public interface AuthenticationMatcher extends HintRegistrations {

  boolean matchAuthentication(Authentication authentication, List<String> subjects);

}
