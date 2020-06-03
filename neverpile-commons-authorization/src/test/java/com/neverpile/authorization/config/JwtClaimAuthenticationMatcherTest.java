package com.neverpile.authorization.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.common.authorization.policy.impl.JwtClaimAuthenticationMatcher;

public class JwtClaimAuthenticationMatcherTest {
  @Test
  public void testThat_matcherDoesNotMatchNonJwtAuthentication() throws Exception {
    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(
        new UsernamePasswordAuthenticationToken("foo", "bar"), Collections.singletonList("foo"))).isFalse();
  }

  @Test
  public void testThat_matcherMatchesAlwaysTrueExpression() throws Exception {
    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(new HashMap<>()),
        Collections.singletonList("claim:true"))).isTrue();
  }

  @Test
  public void testThat_matcherMatchesExistenceOfClaim() throws Exception {
    HashMap<String, Object> claims = new HashMap<>();
    claims.put("foo", "bar");

    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:foo"))).isTrue();

    claims.clear();
    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:foo"))).isFalse();
  }

  @Test
  public void testThat_matcherMatchesBooleanClaim() throws Exception {
    HashMap<String, Object> claims = new HashMap<>();
    claims.put("foo", true);

    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:foo"))).isTrue();

    claims.put("foo", false);
    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:foo"))).isFalse();
  }

  @Test
  public void testThat_matcherMatchesStringClaim() throws Exception {
    HashMap<String, Object> claims = new HashMap<>();
    claims.put("foo", "bar");

    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:foo == 'bar'"))).isTrue();

    claims.put("foo", "baz");
    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:foo == 'bar'"))).isFalse();
  }

  @Test
  public void testThat_matcherMatchesNumericClaim() throws Exception {
    HashMap<String, Object> claims = new HashMap<>();
    claims.put("foo", 4711);

    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:foo > 5"))).isTrue();

    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:foo < 5"))).isFalse();
  }

  @Test
  public void testThat_matcherMatchesInstantClaim() throws Exception {
    HashMap<String, Object> claims = new HashMap<>();
    claims.put("foo", Instant.ofEpochMilli(1000));
    claims.put("bar", Instant.ofEpochMilli(500));
    claims.put("baz", Instant.ofEpochMilli(5000));

    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:bar.isBefore(foo)"))).isTrue();
    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:bar.isAfter(foo)"))).isFalse();
    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:baz.isAfter(foo)"))).isTrue();
    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:baz.isBefore(foo)"))).isFalse();
  }

  @Test
  public void testThat_matcherMatchesJsonClaim() throws Exception {
    HashMap<String, Object> claims = new HashMap<>();
    claims.put("resource_access", new ObjectMapper() //
        .readTree("{ \"neverpile-fusion\": { \"roles\": [\"reader\"]}}"));

    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:resource_access['neverpile-fusion'].roles.contains('reader')"))).isTrue();
    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:resource_access['neverpile-fusion'].roles.contains('writer')"))).isFalse();
  }
  
  @Test
  public void testThat_matcherMatchesHandlesArray() throws Exception {
    HashMap<String, Object> claims = new HashMap<>();
    claims.put("resource_access", new ObjectMapper() //
        .readTree("{ \"neverpile-fusion\": { \"roles\": [\"reader\"]}}"));
    
    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:resource_access['neverpile-fusion'].roles[0] == 'reader'"))).isTrue();
    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:resource_access['neverpile-fusion'].roles[0] == 'writer'"))).isFalse();
  }

  @Test
  public void testThat_matcherHandlesNonexistingJsonPath() throws Exception {
    HashMap<String, Object> claims = new HashMap<>();
    claims.put("resource_access", new ObjectMapper() //
        .readTree("{ \"neverpile-fusion\": { \"roles\": [\"reader\"]}}"));
    
    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:resource_access?.foo?.bar == 'baz'"))).isFalse();
  }
  
  @Test
  public void testThat_matcherHandlesFailedLookups() throws Exception {
    HashMap<String, Object> claims = new HashMap<>();
    
    assertThat(new JwtClaimAuthenticationMatcher().matchAuthentication(makeToken(claims),
        Collections.singletonList("claim:foo.bar == 'baz'"))).isFalse();
  }

  private JwtAuthenticationToken makeToken(final Map<String, Object> claims) {
    Map<String, Object> headers = new HashMap<>();
    headers.put("typ", "JWT");

    if (claims.isEmpty())
      claims.put("dummy", null);

    return new JwtAuthenticationToken(
        new Jwt("foo", Instant.ofEpochMilli(1), Instant.ofEpochMilli(Long.MAX_VALUE), headers, claims));
  }
}
