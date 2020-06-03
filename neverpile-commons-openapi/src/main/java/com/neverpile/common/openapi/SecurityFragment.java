package com.neverpile.common.openapi;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * An {@link OpenApiFragment} used to declare {@code securityScheme}s and endpoint {@code security}.
 * See {@link #withBasicAuth()} and <a href=
 * "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md#securitySchemeObject">the
 * specification</a> for usage details.
 */
public class SecurityFragment extends JsonOpenApiFragment {

  private final ObjectNode schemesNode;
  private final ArrayNode securityNode;

  public SecurityFragment(final String name) {
    super(name);
    schemesNode = getRoot().with("components").with("securitySchemes");
    securityNode = getRoot().withArray("security");
  }

  /**
   * Declare an HTTP basic auth security scheme and reference it for all exposed endpoints.
   * <p>
   * Example usage in Spring:
   * 
   * <pre>
   * &#64;Bean
   * public OpenApiFragment securitySchemeFragment() {
   *   return new SecurityFragment("security").withBasicAuth();
   * }
   * </pre>
   * 
   * @return a security fragment
   */
  public SecurityFragment withBasicAuth() {
    getSchemesNode().with("basicAuth").put("type", "http").put("scheme", "basic");
    getSecurityNode().addObject().withArray("basicAuth");
    return this;
  }

  public enum OAuthFlowType {
    authorizationCode, implicit, password, clientCredentials;
  }

  public class FlowBuilder {

    private final OAuthFragmentBuilder oAuthFragmentBuilder;
    private final ObjectNode flowNode;

    public FlowBuilder(final OAuthFragmentBuilder oAuthFragmentBuilder, final ObjectNode flowNode) {
      this.oAuthFragmentBuilder = oAuthFragmentBuilder;
      this.flowNode = flowNode;
    }

    public FlowBuilder withAuthorizationUrl(final String url) {
      flowNode.put("authorizationUrl", url);
      return this;
    }

    public FlowBuilder withTokenUrl(final String url) {
      flowNode.put("tokenUrl", url);
      return this;
    }

    public FlowBuilder withRefreshUrl(final String url) {
      flowNode.put("refreshUrl", url);
      return this;
    }

    public FlowBuilder withScope(final String scope, final String description) {
      flowNode.with("scopes").put(scope, description);
      return this;
    }

    public OAuthFragmentBuilder complete() {
      return oAuthFragmentBuilder;
    }
  }

  public class OAuthFragmentBuilder {
    private final SecurityFragment securityFragment;
    private final ObjectNode oauthNode;

    public OAuthFragmentBuilder(final SecurityFragment securityFragment, final ObjectNode oauthNode) {
      this.securityFragment = securityFragment;
      this.oauthNode = oauthNode;
    }

    public FlowBuilder withFlow(final OAuthFlowType type) {
      return new FlowBuilder(this, oauthNode.with("flows").with(type.name()));
    }

    public SecurityFragment complete() {
      return securityFragment;
    }
  }

  
  // @formatter:off
   /**
   * Declare an Oauth security scheme and reference it for all exposed endpoints.
   * <p>
   * Example usage in Spring:
   * 
   * <pre>
   * &#64;Bean
   * public OpenApiFragment securitySchemeFragment() {
   *   return new SecurityFragment("security")
   *    .withOAuth()
   *      .withFlow(OAuthFlowType.implicit)
   *        .withScope("read", "read some domain information")
   *        .complete()
   *      .complete();
   * }
   * </pre>
   * 
   * @return a security fragment
   */
  // @formatter:on
  public OAuthFragmentBuilder withOAuth() {
    ObjectNode oauthNode = getSchemesNode().with("oAuth").put("type", "oauth2");
    getSecurityNode().addObject().withArray("oAuth");
    return new OAuthFragmentBuilder(this, oauthNode);
  }

  public ObjectNode getSchemesNode() {
    return schemesNode;
  }

  public ArrayNode getSecurityNode() {
    return securityNode;
  }

  public static void foo() {
    new SecurityFragment(GLOBAL)
      .withOAuth()
        .withFlow(OAuthFlowType.implicit)
          .withScope("read", "read domain information")
          .complete()
        .complete();
  }
}
