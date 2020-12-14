package com.neverpile.common.authorization.policy.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.DataBindingMethodResolver;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
@ConditionalOnClass(JwtAuthenticationToken.class)
public class JwtClaimAuthenticationMatcher implements AuthenticationMatcher {

  private final class NullForMissingEntryMapAccessor extends MapAccessor {
    @Override
    public boolean canRead(final EvaluationContext context, final Object target, final String name)
        throws AccessException {
      return true; // return null for not found
    }

    @Override
    public TypedValue read(final EvaluationContext context, final Object target, final String name)
        throws AccessException {
      return super.canRead(context, target, name) ? super.read(context, target, name) : TypedValue.NULL;
    }
  }

  private final class ObjectNodeAccessor implements PropertyAccessor {
    @Override
    public Class<?>[] getSpecificTargetClasses() {
      return new Class[]{
          ObjectNode.class
      };
    }

    @Override
    public boolean canRead(final EvaluationContext context, final Object target, final String name)
        throws AccessException {
      return true; // return null for not found
    }

    @Override
    public TypedValue read(final EvaluationContext context, final Object target, final String name)
        throws AccessException {
      return new TypedValue(nodeToValue(((ObjectNode) target).path(name)));
    }

    private Object nodeToValue(final JsonNode v) {
      if (v.isMissingNode())
        return null;
      if (v.isTextual())
        return v.asText();
      if (v.isFloatingPointNumber())
        return v.asDouble();
      if (v.isNumber())
        return v.asLong();
      if (v.isBoolean())
        return v.asBoolean();
      if (v.isArray()) {
        ArrayNode a = (ArrayNode)v;
        ArrayList<Object> l = new ArrayList<>(a.size());
        for(JsonNode n : a)
          l.add(nodeToValue(n));
        return l;
      }
      return v;
    }

    @Override
    public boolean canWrite(final EvaluationContext context, final Object target, final String name)
        throws AccessException {
      return false;
    }

    @Override
    public void write(final EvaluationContext context, final Object target, final String name, final Object newValue)
        throws AccessException {
      // nope
    }
  }
  
  private static final Logger LOGGER = LoggerFactory.getLogger(JwtClaimAuthenticationMatcher.class);

  private static final String SUBJECT_PREFIX = "claim:";

  private final ExpressionParser parser = new SpelExpressionParser();

  @Override
  public boolean matchAuthentication(final Authentication authentication, final List<String> subjects) {
    if (authentication instanceof JwtAuthenticationToken) {
      JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;

      for (String subject : subjects) {
        if (subject.startsWith(SUBJECT_PREFIX)) {
          String expressionAsString = subject.substring(SUBJECT_PREFIX.length());
          Expression expression = parser.parseExpression(expressionAsString);

          // expose all claims as variables
          EvaluationContext ctx = SimpleEvaluationContext //
              .forPropertyAccessors(new NullForMissingEntryMapAccessor(), new ObjectNodeAccessor()) //
              .withMethodResolvers(DataBindingMethodResolver.forInstanceMethodInvocation()) //
              .withRootObject(jwtToken.getToken().getClaims()) //
              .build();

          try {
            boolean outcome;
            Object result = expression.getValue(ctx, Object.class);
            if (null == result)
              outcome = false;
            if (result instanceof Boolean)
              outcome = (Boolean) result;
            outcome = true;
            
            LOGGER.debug("  The JWT claims {} the expression {}", outcome ? "SATISFY" : "do not satisfy", expressionAsString);
            
            return outcome;
          } catch (EvaluationException e) {
            LOGGER.warn("Failed to evaluate expression `{}`: {}", expression.getExpressionString(), e.getLocalizedMessage());
          }
        }
      }
    }

    return false;
  }

  @Override
  public List<Hint> getHints() {
    return Arrays.asList(new Hint(SUBJECT_PREFIX, "JWT custom claim (evaluated as a SpEL expression)"));
  }
}
