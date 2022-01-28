package com.neverpile.authorization.policy.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.neverpile.common.authorization.api.Action;
import com.neverpile.common.authorization.api.Permission;
import com.neverpile.common.authorization.policy.AccessPolicy;
import com.neverpile.common.authorization.policy.AccessRule;
import com.neverpile.common.authorization.policy.Effect;
import com.neverpile.common.authorization.policy.PolicyRepository;
import com.neverpile.common.authorization.policy.impl.AuthorityAuthenticationMatcher;
import com.neverpile.common.authorization.policy.impl.EmptyAuthorizationContext;
import com.neverpile.common.authorization.policy.impl.PolicyBasedAuthorizationService;

@Import({
    PolicyBasedAuthorizationService.class, AuthorityAuthenticationMatcher.class
})
@SpringBootTest
public class PolicyBasedAuthorizationServiceTest {
  @Autowired
  PolicyBasedAuthorizationService authService;

  @MockBean
  PolicyRepository mockPolicyRepository;

  private final EmptyAuthorizationContext eac = new EmptyAuthorizationContext();

  @BeforeEach
  public void init() {
    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pass", "USER"));
  }

  @Test
  public void testThat_isAccessAllowedHandlesDefaultEffectAllow() {
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();
      accessPolicy.setDefaultEffect(Effect.ALLOW);

      AccessRule r1 = new AccessRule();
      accessPolicy.getRules().add(r1);

      r1.setEffect(Effect.ALLOW);
      r1.setSubjects(Arrays.asList("*"));
      r1.setResources(Arrays.asList("*"));
      r1.setActions(Arrays.asList("anAllowedAction"));

      AccessRule r2 = new AccessRule();
      accessPolicy.getRules().add(r2);

      r2.setEffect(Effect.DENY);
      r2.setSubjects(Arrays.asList("*"));
      r2.setResources(Arrays.asList("*"));
      r2.setActions(Arrays.asList("aDeniedAction"));

      return accessPolicy;
    });

    assertThat(authService.getPermissions("foo", eac)) //
        .containsExactly(Permission.allow("anAllowedAction"), Permission.deny("aDeniedAction"), Permission.allow("*"));

    assertThat(authService.isAccessAllowed("foo", actionSet("anAllowedAction"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("foo", actionSet("anUnhandledAction"), eac)).isTrue(); // default
                                                                                                  // effect!
    assertThat(authService.isAccessAllowed("foo", actionSet("aDeniedAction"), eac)).isFalse();
  }

  @Test
  public void testThat_isAccessAllowedHandlesMultipleActionsWithDefaultEffectAllow() {
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();
      accessPolicy.setDefaultEffect(Effect.ALLOW);

      AccessRule r1 = new AccessRule();
      accessPolicy.getRules().add(r1);

      r1.setEffect(Effect.ALLOW);
      r1.setSubjects(Arrays.asList("*"));
      r1.setResources(Arrays.asList("*"));
      r1.setActions(Arrays.asList("anAllowedAction"));

      AccessRule r2 = new AccessRule();
      accessPolicy.getRules().add(r2);

      r2.setEffect(Effect.DENY);
      r2.setSubjects(Arrays.asList("*"));
      r2.setResources(Arrays.asList("*"));
      r2.setActions(Arrays.asList("aDeniedAction"));

      AccessRule r3 = new AccessRule();
      accessPolicy.getRules().add(r3);

      r3.setEffect(Effect.ALLOW);
      r3.setSubjects(Arrays.asList("*"));
      r3.setResources(Arrays.asList("*"));
      r3.setActions(Arrays.asList("anotherAllowedAction"));

      return accessPolicy;
    });

    // default effect for anUnhandledAction
    assertThat(authService.isAccessAllowed("foo", actionSet("anAllowedAction", "anUnhandledAction"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("foo", actionSet("aDeniedAction", "anUnhandledAction"), eac)).isFalse();

    assertThat(authService.isAccessAllowed("foo", actionSet("anAllowedAction", "anotherAllowedAction"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("foo", actionSet("aDeniedAction", "anotherAllowedAction"), eac)).isFalse();
  }

  @Test
  public void testThat_isAccessAllowedHandlesMultipleActionsWithDefaultEffectDeny() {
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();
      accessPolicy.setDefaultEffect(Effect.DENY);

      AccessRule r1 = new AccessRule();
      accessPolicy.getRules().add(r1);

      r1.setEffect(Effect.ALLOW);
      r1.setSubjects(Arrays.asList("*"));
      r1.setResources(Arrays.asList("*"));
      r1.setActions(Arrays.asList("anAllowedAction"));

      AccessRule r2 = new AccessRule();
      accessPolicy.getRules().add(r2);

      r2.setEffect(Effect.DENY);
      r2.setSubjects(Arrays.asList("*"));
      r2.setResources(Arrays.asList("*"));
      r2.setActions(Arrays.asList("aDeniedAction"));

      AccessRule r3 = new AccessRule();
      accessPolicy.getRules().add(r3);

      r3.setEffect(Effect.ALLOW);
      r3.setSubjects(Arrays.asList("*"));
      r3.setResources(Arrays.asList("*"));
      r3.setActions(Arrays.asList("anotherAllowedAction"));

      return accessPolicy;
    });

    // default effect for anUnhandledAction
    assertThat(authService.isAccessAllowed("foo", actionSet("anAllowedAction", "anUnhandledAction"), eac)).isFalse();
    assertThat(authService.isAccessAllowed("foo", actionSet("aDeniedAction", "anUnhandledAction"), eac)).isFalse();

    assertThat(authService.isAccessAllowed("foo", actionSet("anAllowedAction", "anotherAllowedAction"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("foo", actionSet("aDeniedAction", "anotherAllowedAction"), eac)).isFalse();
  }

  @Test
  public void testThat_isAccessAllowedHandlesDefaultEffectDeny() {
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();
      accessPolicy.setDefaultEffect(Effect.DENY);

      AccessRule r1 = new AccessRule();
      accessPolicy.getRules().add(r1);

      r1.setEffect(Effect.ALLOW);
      r1.setSubjects(Arrays.asList("*"));
      r1.setResources(Arrays.asList("*"));
      r1.setActions(Arrays.asList("anAllowedAction"));

      AccessRule r2 = new AccessRule();
      accessPolicy.getRules().add(r2);

      r2.setEffect(Effect.DENY);
      r2.setSubjects(Arrays.asList("*"));
      r2.setResources(Arrays.asList("*"));
      r2.setActions(Arrays.asList("aDeniedAction"));

      return accessPolicy;
    });

    assertThat(authService.isAccessAllowed("foo", actionSet("anAllowedAction"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("foo", actionSet("anUnhandledAction"), eac)).isFalse(); // default
                                                                                                   // effect!
    assertThat(authService.isAccessAllowed("foo", actionSet("aDeniedAction"), eac)).isFalse(); // default
                                                                                               // effect!
  }

  @Test
  public void testThat_evaluationConsidersAuthenticatedStatus() {
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();
      accessPolicy.setDefaultEffect(Effect.DENY);

      AccessRule r1 = new AccessRule();
      accessPolicy.getRules().add(r1);

      r1.setName("r1");
      r1.setEffect(Effect.ALLOW);
      r1.setSubjects(Arrays.asList("authenticated"));
      r1.setResources(Arrays.asList("*"));
      r1.setActions(Arrays.asList("allowedForAuthenticated"));

      AccessRule r2 = new AccessRule();
      accessPolicy.getRules().add(r2);

      r2.setName("r2");
      r2.setEffect(Effect.ALLOW);
      r2.setSubjects(Arrays.asList("*"));
      r2.setResources(Arrays.asList("*"));
      r2.setActions(Arrays.asList("allowedForAnyone"));

      return accessPolicy;
    });

    assertThat(authService.getPermissions("foo", eac)) //
        .containsExactly(Permission.allow("allowedForAuthenticated", "allowedForAnyone"));

    assertThat(
        authService.isAccessAllowed("foo", actionSet("allowedForAuthenticated", "allowedForAnyone"), eac)).isTrue();

    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("noone", "noone"));

    assertThat(authService.getPermissions("foo", eac)).containsExactly(Permission.allow("allowedForAnyone"));

    assertThat(authService.isAccessAllowed("foo", actionSet("allowedForAnyone"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("foo", actionSet("allowedForAuthenticated"), eac)).isFalse();
  }

  private Set<Action> actionSet(final String... keys) {
    HashSet<Action> result = new HashSet<Action>();
    for (String k : keys) {
      result.add(Action.of(k));
    }
    return result;
  }

  @Test
  public void testThat_evaluationConsidersPrincipal() {
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();
      accessPolicy.setDefaultEffect(Effect.DENY);

      AccessRule r1 = new AccessRule();
      accessPolicy.getRules().add(r1);

      r1.setEffect(Effect.ALLOW);
      r1.setSubjects(Arrays.asList("principal:user"));
      r1.setResources(Arrays.asList("*"));
      r1.setActions(Arrays.asList("allowedForUser"));

      AccessRule r2 = new AccessRule();
      accessPolicy.getRules().add(r2);

      r2.setEffect(Effect.ALLOW);
      r2.setSubjects(Arrays.asList("*"));
      r2.setResources(Arrays.asList("*"));
      r2.setActions(Arrays.asList("allowedForAnyone"));

      return accessPolicy;
    });

    assertThat(authService.getPermissions("foo", eac)).containsExactly(
        Permission.allow("allowedForUser", "allowedForAnyone"));
    assertThat(authService.isAccessAllowed("foo", actionSet("allowedForUser", "allowedForAnyone"), eac)).isTrue();

    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("johndoe", "", "USER"));

    assertThat(authService.getPermissions("foo", eac)).containsExactly(Permission.allow("allowedForAnyone"));
    assertThat(authService.isAccessAllowed("foo", actionSet("allowedForAnyone"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("foo", actionSet("allowedForUser", "allowedForAnyone"), eac)).isFalse();
  }

  @Test
  public void testThat_evaluationConsidersRole() {
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();

      AccessRule r1 = new AccessRule();
      accessPolicy.getRules().add(r1);

      r1.setEffect(Effect.ALLOW);
      r1.setSubjects(Arrays.asList("role:USER"));
      r1.setResources(Arrays.asList("*"));
      r1.setActions(Arrays.asList("allowedForRoleUSER"));

      AccessRule r2 = new AccessRule();
      accessPolicy.getRules().add(r2);

      r2.setEffect(Effect.ALLOW);
      r2.setSubjects(Arrays.asList("*"));
      r2.setResources(Arrays.asList("*"));
      r2.setActions(Arrays.asList("allowedForAnyone"));

      return accessPolicy;
    });

    assertThat(authService.getPermissions("foo", eac)) //
        .containsExactly(Permission.allow("allowedForRoleUSER", "allowedForAnyone"));

    assertThat(authService.isAccessAllowed("foo", actionSet("allowedForRoleUSER", "allowedForAnyone"), eac)).isTrue();

    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "", "A_ROLE"));

    assertThat(authService.getPermissions("foo", eac)).containsExactly(Permission.allow("allowedForAnyone"));

    assertThat(authService.isAccessAllowed("foo", actionSet("allowedForAnyone"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("foo", actionSet("allowedForRoleUSER", "allowedForAnyone"), eac)).isFalse();
  }

  @Test
  public void testThat_evaluationConsidersResource() {
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();

      AccessRule r1 = new AccessRule();
      accessPolicy.getRules().add(r1);

      r1.setEffect(Effect.ALLOW);
      r1.setSubjects(Arrays.asList("*"));
      r1.setResources(Arrays.asList("foo"));
      r1.setActions(Arrays.asList("allowedInFoo"));

      AccessRule r2 = new AccessRule();
      accessPolicy.getRules().add(r2);

      r2.setEffect(Effect.ALLOW);
      r2.setSubjects(Arrays.asList("*"));
      r2.setResources(Arrays.asList("bar"));
      r2.setActions(Arrays.asList("allowedInBar"));

      return accessPolicy;
    });

    assertThat(authService.getPermissions("foo", eac)).containsExactly(Permission.allow("allowedInFoo"));
    assertThat(authService.getPermissions("bar", eac)).containsExactly(Permission.allow("allowedInBar"));
    assertThat(authService.getPermissions("baz", eac)).isEmpty();

    assertThat(authService.isAccessAllowed("foo", actionSet("allowedInFoo"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("foo", actionSet("allowedInBar"), eac)).isFalse();
    assertThat(authService.isAccessAllowed("bar", actionSet("allowedInFoo"), eac)).isFalse();
    assertThat(authService.isAccessAllowed("bar", actionSet("allowedInBar"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("baz", actionSet("allowedInFoo"), eac)).isFalse();
    assertThat(authService.isAccessAllowed("baz", actionSet("allowedInBar"), eac)).isFalse();
  }

  @Test
  public void testThat_evaluationConsidersEffect() {
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();

      AccessRule r1 = new AccessRule();
      accessPolicy.getRules().add(r1);

      r1.setEffect(Effect.DENY);
      r1.setSubjects(Arrays.asList("*"));
      r1.setResources(Arrays.asList("foo"));
      r1.setActions(Arrays.asList("explicitlyDenied", "deniedByWildcard:*", "deniedByWildcardWithSub:deny:*"));

      AccessRule r2 = new AccessRule();
      accessPolicy.getRules().add(r2);

      r2.setEffect(Effect.ALLOW);
      r2.setSubjects(Arrays.asList("*"));
      r2.setResources(Arrays.asList("foo"));
      r2.setActions(Arrays.asList("explicitlyAllowed", //
          "deniedByWildcard:foo", "deniedByWildcard:foo:bar", //
          "deniedByWildcardWithSub:allow:foo", //
          "deniedByWildcardWithSub:deny:foo"));

      return accessPolicy;
    });

    assertThat(authService.getPermissions("foo", eac)) //
        .containsExactly(Permission.deny("explicitlyDenied", "deniedByWildcard:*", "deniedByWildcardWithSub:deny:*"),
            Permission.allow("explicitlyAllowed", //
                "deniedByWildcard:foo", "deniedByWildcard:foo:bar", //
                "deniedByWildcardWithSub:allow:foo", //
                "deniedByWildcardWithSub:deny:foo"));

    assertThat(authService.isAccessAllowed("foo", actionSet("explicitlyAllowed"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("foo", actionSet("deniedByWildcardWithSub:allow:foo"), eac)).isTrue();

    assertThat(authService.isAccessAllowed("foo", actionSet("explicitlyDenied"), eac)).isFalse();
    assertThat(authService.isAccessAllowed("foo", actionSet("deniedByWildcard:bar"), eac)).isFalse();
    assertThat(authService.isAccessAllowed("foo", actionSet("deniedByWildcard:bar:baz"), eac)).isFalse();
  }
}
