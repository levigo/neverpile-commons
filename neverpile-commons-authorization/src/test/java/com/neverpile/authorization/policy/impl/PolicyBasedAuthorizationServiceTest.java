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

  @Test
  public void testThat_allActionsAreDeniedIfDefaultEffectIsDenyAndNoMatchingRule() {
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();
      accessPolicy.setDefaultEffect(Effect.DENY);

      AccessRule rule = new AccessRule();
      rule.setEffect(Effect.ALLOW);
      rule.setSubjects(Arrays.asList("authenticated"));
      rule.setResources(Arrays.asList("*"));
      rule.setActions(Arrays.asList("*"));
      accessPolicy.getRules().add(rule);

      return accessPolicy;
    });

    // Simulate an unauthenticated user
    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("noone", "noone"));

    // All actions should be denied for unauthenticated user
    assertThat(authService.isAccessAllowed("foo", actionSet("anyAction"), eac)).isFalse();
    assertThat(authService.isAccessAllowed("foo", actionSet("anotherAction"), eac)).isFalse();
    assertThat(authService.isAccessAllowed("foo", actionSet("read", "write", "delete"), eac)).isFalse();

    // Simulate an authenticated user
    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pass", "USER"));

    // All actions should be allowed for authenticated user (covered by rule)
    assertThat(authService.isAccessAllowed("foo", actionSet("anyAction"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("foo", actionSet("read", "write", "delete"), eac)).isTrue();
  }

  @Test
  public void testThat_defaultEffectDenyAndNoRulesMeansAllDenied() {
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();
      accessPolicy.setDefaultEffect(Effect.DENY);
      // No rules
      return accessPolicy;
    });

    assertThat(authService.isAccessAllowed("foo", actionSet("read"), eac)).isFalse();
    assertThat(authService.isAccessAllowed("foo", actionSet("write"), eac)).isFalse();
    assertThat(authService.isAccessAllowed("foo", actionSet("delete"), eac)).isFalse();
  }

  @Test
  public void testThat_defaultEffectAllowAndNoRulesMeansAllAllowed() {
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();
      accessPolicy.setDefaultEffect(Effect.ALLOW);
      // No rules
      return accessPolicy;
    });

    assertThat(authService.isAccessAllowed("foo", actionSet("read"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("foo", actionSet("write"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("foo", actionSet("delete"), eac)).isTrue();
  }

  @Test
  public void testThat_explicitDenyOverridesDefaultAllow() {
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();
      accessPolicy.setDefaultEffect(Effect.ALLOW);

      AccessRule denyRule = new AccessRule();
      denyRule.setEffect(Effect.DENY);
      denyRule.setSubjects(Arrays.asList("*"));
      denyRule.setResources(Arrays.asList("*"));
      denyRule.setActions(Arrays.asList("delete"));
      accessPolicy.getRules().add(denyRule);

      return accessPolicy;
    });

    assertThat(authService.isAccessAllowed("foo", actionSet("read"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("foo", actionSet("delete"), eac)).isFalse();
    assertThat(authService.isAccessAllowed("foo", actionSet("write", "delete"), eac)).isFalse();
  }

  @Test
  public void testThat_explicitAllowOverridesDefaultDeny() {
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();
      accessPolicy.setDefaultEffect(Effect.DENY);

      AccessRule allowRule = new AccessRule();
      allowRule.setEffect(Effect.ALLOW);
      allowRule.setSubjects(Arrays.asList("*"));
      allowRule.setResources(Arrays.asList("*"));
      allowRule.setActions(Arrays.asList("read"));
      accessPolicy.getRules().add(allowRule);

      return accessPolicy;
    });

    assertThat(authService.isAccessAllowed("foo", actionSet("read"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("foo", actionSet("write"), eac)).isFalse();
    assertThat(authService.isAccessAllowed("foo", actionSet("read", "write"), eac)).isFalse();
  }

  @Test
  public void testThat_multipleRulesFirstMatchWins() {
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();
      accessPolicy.setDefaultEffect(Effect.DENY);

      AccessRule denyRule = new AccessRule();
      denyRule.setEffect(Effect.DENY);
      denyRule.setSubjects(Arrays.asList("*"));
      denyRule.setResources(Arrays.asList("*"));
      denyRule.setActions(Arrays.asList("read"));
      accessPolicy.getRules().add(denyRule);

      AccessRule allowRule = new AccessRule();
      allowRule.setEffect(Effect.ALLOW);
      allowRule.setSubjects(Arrays.asList("*"));
      allowRule.setResources(Arrays.asList("*"));
      allowRule.setActions(Arrays.asList("read", "write"));
      accessPolicy.getRules().add(allowRule);

      return accessPolicy;
    });

    // "read" matches first rule (DENY), so denied
    assertThat(authService.isAccessAllowed("foo", actionSet("read"), eac)).isFalse();
    // "write" matches second rule (ALLOW), so allowed
    assertThat(authService.isAccessAllowed("foo", actionSet("write"), eac)).isTrue();
    // "read" and "write" together: since "read" is denied, overall should be denied
    assertThat(authService.isAccessAllowed("foo", actionSet("read", "write"), eac)).isFalse();
  }

  @Test
  public void testThat_policyAllowsAllActionsForAuthenticatedUser() {
    // This matches the provided JSON policy: allow everything for "authenticated", default DENY
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();
      accessPolicy.setDefaultEffect(Effect.DENY);

      AccessRule rule = new AccessRule();
      rule.setEffect(Effect.ALLOW);
      rule.setSubjects(Arrays.asList("authenticated"));
      rule.setResources(Arrays.asList("*"));
      rule.setActions(Arrays.asList("*"));
      accessPolicy.getRules().add(rule);

      return accessPolicy;
    });

    // Simulate an authenticated user
    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pass", "USER"));

    // Test standard CRUD actions
    assertThat(authService.isAccessAllowed("document", actionSet("read"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("document", actionSet("write"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("document", actionSet("update"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("document", actionSet("delete"), eac)).isTrue();

    // Test custom actions
    assertThat(authService.isAccessAllowed("document", actionSet("share"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("document", actionSet("archive"), eac)).isTrue();

    // Test actions with namespaces
    assertThat(authService.isAccessAllowed("document", actionSet("document:metadata:read"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("document", actionSet("workflow:approve"), eac)).isTrue();
  }

  @Test
  public void testThat_modifiedPolicyAllowsSpecificActionsOnly() {
    // Modified JSON policy: allow only read and metadata actions for authenticated users
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();
      accessPolicy.setDefaultEffect(Effect.DENY);

      AccessRule rule = new AccessRule();
      rule.setEffect(Effect.ALLOW);
      rule.setSubjects(Arrays.asList("authenticated"));
      rule.setResources(Arrays.asList("*"));
      rule.setActions(Arrays.asList("read", "document:metadata:*"));
      accessPolicy.getRules().add(rule);

      return accessPolicy;
    });

    // Simulate an authenticated user
    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pass", "USER"));

    // Test allowed actions
    assertThat(authService.isAccessAllowed("document", actionSet("read"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("document", actionSet("document:metadata:read"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("document", actionSet("document:metadata:write"), eac)).isTrue();

    // Test denied actions
    assertThat(authService.isAccessAllowed("document", actionSet("write"), eac)).isFalse();
    assertThat(authService.isAccessAllowed("document", actionSet("delete"), eac)).isFalse();
    assertThat(authService.isAccessAllowed("document", actionSet("workflow:approve"), eac)).isFalse();

    // Test combinations
    assertThat(authService.isAccessAllowed("document", actionSet("read", "document:metadata:write"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("document", actionSet("read", "write"), eac)).isFalse();
  }

  @Test
  public void testThat_roleBasedPolicyWorksWithActions() {
    // Role-based policy with different action permissions for different roles
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();
      accessPolicy.setDefaultEffect(Effect.DENY);

      // Admin role can do everything
      AccessRule adminRule = new AccessRule();
      adminRule.setEffect(Effect.ALLOW);
      adminRule.setSubjects(Arrays.asList("role:ADMIN"));
      adminRule.setResources(Arrays.asList("*"));
      adminRule.setActions(Arrays.asList("*"));
      accessPolicy.getRules().add(adminRule);

      // User role can only read and view metadata
      AccessRule userRule = new AccessRule();
      userRule.setEffect(Effect.ALLOW);
      userRule.setSubjects(Arrays.asList("role:USER"));
      userRule.setResources(Arrays.asList("*"));
      userRule.setActions(Arrays.asList("read", "document:metadata:read"));
      accessPolicy.getRules().add(userRule);

      return accessPolicy;
    });

    // Test admin role
    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "pass", "ADMIN"));
    assertThat(authService.isAccessAllowed("document", actionSet("read", "write", "delete"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("document", actionSet("document:metadata:write"), eac)).isTrue();

    // Test user role
    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pass", "USER"));
    assertThat(authService.isAccessAllowed("document", actionSet("read"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("document", actionSet("document:metadata:read"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("document", actionSet("write"), eac)).isFalse();
    assertThat(authService.isAccessAllowed("document", actionSet("document:metadata:write"), eac)).isFalse();

    // Test combination for user role
    assertThat(authService.isAccessAllowed("document", actionSet("read", "document:metadata:read"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("document", actionSet("read", "write"), eac)).isFalse();
  }

  @Test
  public void testThat_hierarchicalActionsWorkCorrectly() {
    // Test hierarchical action patterns with wildcards
    given(mockPolicyRepository.getCurrentPolicy()).will(i -> {
      AccessPolicy accessPolicy = new AccessPolicy();
      accessPolicy.setDefaultEffect(Effect.DENY);

      AccessRule rule = new AccessRule();
      rule.setEffect(Effect.ALLOW);
      rule.setSubjects(Arrays.asList("authenticated"));
      rule.setResources(Arrays.asList("*"));
      rule.setActions(Arrays.asList("document:metadata:*", "workflow:*"));
      accessPolicy.getRules().add(rule);

      return accessPolicy;
    });

    // Simulate an authenticated user
    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pass", "USER"));

    // Test wildcard matches
    assertThat(authService.isAccessAllowed("document", actionSet("document:metadata:read"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("document", actionSet("document:metadata:write"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("document", actionSet("workflow:start"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("document", actionSet("workflow:approve"), eac)).isTrue();

    // Test non-matching actions
    assertThat(authService.isAccessAllowed("document", actionSet("read"), eac)).isFalse();
    assertThat(authService.isAccessAllowed("document", actionSet("document:content:read"), eac)).isFalse();

    // Test combinations
    assertThat(authService.isAccessAllowed("document",
        actionSet("document:metadata:read", "workflow:approve"), eac)).isTrue();
    assertThat(authService.isAccessAllowed("document",
        actionSet("document:metadata:read", "read"), eac)).isFalse();
  }
}
