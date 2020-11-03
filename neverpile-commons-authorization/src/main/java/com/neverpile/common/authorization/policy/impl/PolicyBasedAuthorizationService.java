package com.neverpile.common.authorization.policy.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import com.neverpile.common.authorization.api.Action;
import com.neverpile.common.authorization.api.AuthorizationContext;
import com.neverpile.common.authorization.api.AuthorizationService;
import com.neverpile.common.authorization.policy.AccessPolicy;
import com.neverpile.common.authorization.policy.AccessRule;
import com.neverpile.common.authorization.policy.Effect;
import com.neverpile.common.authorization.policy.PolicyRepository;
import com.neverpile.common.condition.CoreConditionRegistry;
import com.neverpile.common.condition.config.ConditionModule;

/**
 *
 */
@Component
@Import({
    CoreConditionRegistry.class, ConditionModule.class, AuthorityAuthenticationMatcher.class,
    JwtClaimAuthenticationMatcher.class
})
public class PolicyBasedAuthorizationService implements AuthorizationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyBasedAuthorizationService.class);

  @Autowired
  private PolicyRepository policyRepository;

  @Autowired
  private List<AuthenticationMatcher> authenticationMatchers;

  @Override
  public boolean isAccessAllowed(final String resourceSpecifier, final Set<Action> actions,
      final AuthorizationContext context) {
    AccessPolicy policy = policyRepository.getCurrentPolicy();

    return isAccessAllowed(resourceSpecifier, actions, context, policy);
  }

  /**
   * Request whether the access specified by the given resource specifier, the set of
   * {@link Action}s within the given {@link AuthorizationContext} shall be permitted or not by the
   * given policy.
   *
   * @param resourceSpecifier the resource specifier indicating the targeted resource
   * @param actions the actions that have been requested (or should be checked)
   * @param context the context of the request
   * @param policy the policy to use for the access control decision
   * @return <code>true</code> if the access shall be allowed, <code>false</code> otherwise
   * @see #isAccessAllowed(String, Set, AuthorizationContext)
   */
  public boolean isAccessAllowed(final String resourceSpecifier, final Set<Action> actions,
      final AuthorizationContext context, final AccessPolicy policy) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    List<AccessRule> matchingRules = policy.getRules().stream() //
        .filter(authentication.isAuthenticated() //
            ? (r) -> matchesAuthentication(r, authentication) //
            : this::matchesAnonymousUser) //
        .filter(r -> matchesResource(r, resourceSpecifier)) //
        .filter(r -> satisfiesConditions(r, context)) //
        .collect(Collectors.toList());

    // match each action individually
    boolean allMatched = true;
    for (Action a : actions) {
      Optional<AccessRule> matchingRule = matchingRules.stream() //
          .filter(r -> matchesActions(a.key(), r.getActions())) //
          .findFirst();
      if (matchingRule.isPresent() && matchingRule.get().getEffect() == Effect.DENY) {
        // deny means deny
        return false;
      }
      allMatched &= matchingRule.isPresent();
    }

    // evaluate applicable effect: if we had explicit matches for all rules, we allow, otherwise we
    // revert to the default effect
    Effect e = allMatched ? Effect.ALLOW : //
        policy.getDefaultEffect() != null ? policy.getDefaultEffect() : Effect.DENY;

    LOGGER.debug("Authorization for {} on {} with principal {}: {}", actions, resourceSpecifier,
        authentication.isAuthenticated() ? authentication.getPrincipal() : "anonymous", e);

    return e == Effect.ALLOW;
  }

  @Override
  public Set<String> getAllowedActions(final String resourceSpecifier, final AuthorizationContext context) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    final Set<String> deniedActions = new HashSet<>();

    return policyRepository.getCurrentPolicy().getRules().stream() //
        .filter(null != authentication && authentication.isAuthenticated() //
            ? (r) -> matchesAuthentication(r, authentication) //
            : this::matchesAnonymousUser) //
        .filter(r -> matchesResource(r, resourceSpecifier)) //
        .filter(r -> satisfiesConditions(r, context)) //
        .sequential() //
        .flatMap(r -> {
          if (r.getEffect() == Effect.ALLOW) {
            // filter out all previously denied actions
            return r.getActions().stream().filter(a -> !matchesActions(a, deniedActions));
          } else {
            // just remember the actions as denied
            deniedActions.addAll(r.getActions());
            return Stream.<String> of();
          }
        }) //
        .collect(Collectors.toSet());
  }

  private boolean matchesAuthentication(final AccessRule rule, final Authentication authentication) {
    List<String> subjects = rule.getSubjects();

    if (subjects.contains(AccessRule.ANY) || subjects.contains(AccessRule.AUTHENTICATED)) {
      LOGGER.debug("  Rule '{}' matches any/any authenticated user", rule.getName());
      return true;
    }

    if (subjects.contains(AccessRule.PRINCIPAL + authentication.getName())) {
      LOGGER.debug("  Rule '{}' matches the authenticated principal {}", rule.getName(), authentication.getName());
      return true;
    }

    boolean m = authenticationMatchers.stream().anyMatch(am -> am.matchAuthentication(authentication, subjects));

    if (LOGGER.isDebugEnabled())
      LOGGER.debug("  Rule '{}' {} the authenticated principal {} with authorities", rule.getName(),
          m ? "matches" : "does not match", authentication.getName(),
          authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(
              Collectors.joining(",")));

    return m;
  }

  private boolean matchesAnonymousUser(final AccessRule rule) {
    boolean m = rule.getSubjects().contains(AccessRule.ANY) || rule.getSubjects().contains(AccessRule.ANONYMOUS_CALLER);

    LOGGER.debug("  Rule '{}' {} the anonymous user", rule.getName(), m ? "matches" : "does not match");

    return m;
  }

  /**
   * <p>
   * An access rule matches a resource specifier if any of the resource patterns in the rule match
   * the given specifier. Matching is performed ant-style, but using periods (".") as the path
   * separator.
   *
   * <h3>Examples</h3>
   * <ul>
   * <li>{@code document.metadata.ba?} &mdash; matches {@code document.metadata.bar} but also
   * {@code document.metadata.baz}</li>
   * <li>{@code document.metadata.*-claims} &mdash; matches all {@code metadata} elements with names
   * ending with {@code -claims}</li>
   * <li>{@code document.**} &mdash; matches all sub-resources of documents</li>
   * </ul>
   *
   * @param rule for which to test the match
   * @param resourceSpecifier the resource specificer to match
   * @return <code>true</code> if a match was found
   * @See {@link AntPathMatcher}
   */
  private boolean matchesResource(final AccessRule rule, final String resourceSpecifier) {
    boolean m = rule.getResources().stream().anyMatch(r -> matchesResource(r, resourceSpecifier));

    LOGGER.debug("  Rule '{}' {} the resource {}", rule.getName(), m ? "matches" : "does not match", resourceSpecifier);

    return m;
  }

  /**
   * An ant-style matcher using periods as path separators.
   */
  private final AntPathMatcher resourcePatternMatcher = new AntPathMatcher(".");

  private boolean matchesResource(final String resourcePattern, final String resourceSpecifier) {
    return resourcePatternMatcher.match(resourcePattern + ".**", resourceSpecifier);
  }

  private boolean matchesActions(final AccessRule rule, final Set<Action> actions) {
    boolean m = actions.stream().allMatch(a -> matchesActions(a.key(), rule.getActions()))
        || rule.getActions().contains(Action.ANY.key());

    LOGGER.debug("  Rule '{}' {} the actions ", rule.getName(), m ? "matches" : "does not match", actions);

    return m;
  }

  /**
   * Match an action keyagainst a list of allowed actions. The matching rules are:
   * <ul>
   * <li>Allowed action <code>*</code> ({@link Action#ANY}) matches any action key.
   * <li>Allowed action <code>ACTION</code> matches key <code>ACTION</code>,
   * <code>NAMESPACE:SUB:ACTION</code> matches key <code>NAMESPACE:SUB:ACTION</code> etc. (trivial
   * key equality).
   * <li><code>NAMESPACE:*</code> matches all keys starting with <code>NAMESPACE:</code>,
   * <code>NAMESPACE:SUB:*</code> matches all keys starting with <code>NAMESPACE:SUB:</code> etc.
   * (trailing wildcard match).
   * <ul>
   * 
   * @param key the action key
   * @param actions the allowed action patterns
   * @return <code>true</code> if the action matches
   */
  private boolean matchesActions(final String key, final Collection<String> actions) {
    return actions.contains(Action.ANY.key()) || actions.contains(key) || matchesWildcardAction(key, actions);
  }

  /**
   * Match an action key using trailing wildcards: {@code NAMESPACE:*} matches all Actions with keys
   * starting with {@code NAMESPACE:}, {@code NAMESPACE:SUB:*} matches all Actions with keys
   * starting with {@code NAMESPACE:SUB:} etc.
   * 
   * @param key the action key
   * @param actions the allowed action patterns
   * @return <code>true</code> if the action matches
   */
  private boolean matchesWildcardAction(final String key, final Collection<String> actions) {
    StringBuilder sb = new StringBuilder(key.length());
    for (String part : key.split(":")) {
      sb.append(part);
      sb.append(":*");

      if (actions.contains(sb.toString()))
        return true;

      sb.deleteCharAt(sb.length() - 1);
    }

    return false;
  }

  private boolean satisfiesConditions(final AccessRule rule, final AuthorizationContext conditionContext) {
    boolean m = rule.getConditions().matches(conditionContext);

    LOGGER.debug("  Rule '{}' the context {} the conditions {}", rule.getName(), m ? "satisfies" : "does not satisfy",
        rule.getConditions());

    return m;
  }
}
