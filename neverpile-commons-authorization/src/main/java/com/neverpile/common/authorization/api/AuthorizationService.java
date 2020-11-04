package com.neverpile.common.authorization.api;

import java.util.List;
import java.util.Set;

import org.springframework.boot.web.servlet.server.Session;
import org.springframework.http.HttpRequest;

import com.neverpile.common.authorization.policy.AccessPolicy;
import com.neverpile.common.authorization.policy.Effect;

/**
 * Implementation of this interface are responsible for making and/or delegating authorization
 * decisions related to a requested set of {@link Action}s on some resource. Authorization decisions
 * can additionally be based on arbitrary contextual information supplied by an
 * {@link AuthorizationContext}.
 * <p>
 * Further sources of input for decisions will usually be rights, roles scopes etc. of the principal
 * attempting the access as well as possibly other information like he current {@link HttpRequest},
 * the {@link Session}, configuration information (e.g. an {@link AccessPolicy}) or other factors.
 * However, these sources are not mandated by this interface and must thus be propagated by other
 * means.
 */
public interface AuthorizationService {
  /**
   * Request whether the access specified by the given resource specifier, the set of
   * {@link Action}s within the given {@link AuthorizationContext} shall be permitted or not.
   * 
   * @param resourceSpecifier the resource specifier indicating the targeted resource
   * @param actions the actions that have been requested (or should be checked)
   * @param context the context of the request
   * @return <code>true</code> if the access shall be allowed, <code>false</code> otherwise
   */
  boolean isAccessAllowed(String resourceSpecifier, Set<Action> actions, AuthorizationContext context);

  /**
   * Retrieve the list of permissions that are applicable for the given resource in the given
   * context. {@link Permission}s consist of a list of action keys or action key patterns along with
   * an {@link Effect} to be caused by a match. The algorithm for the evaluation of the permissions
   * is as follows:
   * <ul>
   * <li>Test the intended action against each permission in sequence until a match is found.
   * <li>As soon as a match is found, use the permission's effect as the outcome of the decision.
   * <li>If no match is found, deny the action.
   * </ul>
   * The access policy's default effect ({@link AccessPolicy#getDefaultEffect()}) is automatically
   * included in the permissions by adding a final permission allowing all actions if the default
   * effect is {@link Effect#ALLOW}.
   * 
   * @param resourceSpecifier the resource specifier indicating the targeted resource
   * @param context the context of the request
   * @return the applicable permissions
   */
  List<Permission> getPermissions(String resourceSpecifier, AuthorizationContext context);
}
