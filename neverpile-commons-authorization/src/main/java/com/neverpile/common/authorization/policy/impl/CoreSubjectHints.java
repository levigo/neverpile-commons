package com.neverpile.common.authorization.policy.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.neverpile.common.authorization.api.HintRegistrations;
import com.neverpile.common.authorization.policy.AccessRule;
import com.neverpile.common.authorization.policy.SubjectHints;

/**
 * {@link HintRegistrations} for the core subject patterns.
 */
@Component
@SubjectHints
public class CoreSubjectHints implements HintRegistrations {
  @Override
  public List<Hint> getHints() {
    return Arrays.asList( //
        new Hint(AccessRule.ANY, "anything"), //
        new Hint(AccessRule.AUTHENTICATED, "any authenticated principal"), //
        new Hint(AccessRule.PRINCIPAL, "name of a principal (e.g. user id)"), //
        new Hint(AccessRule.ANONYMOUS_CALLER, "anonymous") //
    );
  }
}
