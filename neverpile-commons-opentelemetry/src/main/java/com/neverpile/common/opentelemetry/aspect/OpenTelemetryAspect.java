package com.neverpile.common.opentelemetry.aspect;

import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.neverpile.common.opentelemetry.Attribute;
import com.neverpile.common.opentelemetry.Attribute.AttributeExtractor;
import com.neverpile.common.opentelemetry.TraceInvocation;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import jakarta.annotation.PostConstruct;

/**
 * An aspect used to create OpenTelemetry spans for calls to methods annotated with
 * {@link TraceInvocation}.
 */
@Aspect
public class OpenTelemetryAspect {
  private static final Logger LOGGER = LoggerFactory.getLogger(OpenTelemetryAspect.class);

  private final Tracer tracer;

  public OpenTelemetryAspect(Tracer tracer) {
    this.tracer = tracer;
  }

  private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

  private final Map<Class<? extends Function<Object, Object>>, Function<Object, Object>> valueAdapterCache = new ConcurrentHashMap<>();

  private final Map<Class<? extends AttributeExtractor<Object>>, AttributeExtractor<Object>> attributeExtractorCache = new ConcurrentHashMap<>();

  @PostConstruct
  public void logActivation() {
    LOGGER.info("OpenTelemetry Tracer found - tracing of methods annotated with @TraceInvocation enabled");
  }

  @Around("execution (@com.neverpile.common.opentelemetry.TraceInvocation * *.*(..))")
  public Object newSpanAround(final ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Object[] args = joinPoint.getArgs();

    Span span = startSpan(signature, joinPoint.getThis());

    resolveParameters(signature, args, span);

    try (Scope scope = span.makeCurrent()) {
      return joinPoint.proceed(args);
    } catch (Throwable ex) {
      span.setStatus(StatusCode.ERROR, ex.getMessage());
      span.recordException(ex);
      throw ex;
    } finally {
      span.end();
    }
  }

  private void resolveParameters(final MethodSignature signature, final Object[] args, final Span span) {
    Parameter[] parameters = signature.getMethod().getParameters();
    for (int i = 0; i < parameters.length; i++) {
      if (parameters[i].getAnnotation(Attribute.class) != null) {
        setupAttribute(signature, i, parameters[i], args[i], span);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void setupAttribute(final MethodSignature signature, final int parameterIndex, final Parameter parameter,
      final Object arg, final Span span) {
    Attribute annotation = parameter.getAnnotation(Attribute.class);

    // if we have a value extractor, use it
    if (!annotation.attributeExtractor().equals(Attribute.NoopExtractor.class)) {
      if (!annotation.valueAdapter().equals(Attribute.NoopMapper.class))
        LOGGER.warn("@Attribute.attributeExtractor and @Attribute.valueAdapter are mutually exclusive");
      attributeExtractorCache //
          .computeIfAbsent((Class<? extends AttributeExtractor<Object>>) annotation.attributeExtractor(),
              c -> (AttributeExtractor<Object>) BeanUtils.instantiateClass(c)) //
          .extract(arg, (key, value) -> setAttribute(span, key, value));

    } else {
      // otherwise build a single attribute
      String attributeKey = StringUtils.hasText(annotation.name())
          ? annotation.name()
          : findParameterName(signature, parameterIndex);
      Object value = arg;

      // if we have a value mapper, apply it to the value
      if (!annotation.valueAdapter().equals(Attribute.NoopMapper.class)) {
        value = valueAdapterCache //
            .computeIfAbsent((Class<? extends Function<Object, Object>>) annotation.valueAdapter(),
                c -> (Function<Object, Object>) BeanUtils.instantiateClass(c)) //
            .apply(value);
      }

      setAttribute(span, attributeKey, value);
    }
  }

  private String findParameterName(final MethodSignature signature, final int parameterIndex) {
    String[] parameterNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
    if (null == parameterNames || parameterNames.length <= parameterIndex) {
      LOGGER.warn("Can't determine a parameter name for {}", signature.getMethod());
      return "<unknown>";
    }

    return parameterNames[parameterIndex];
  }

  private void setAttribute(final Span span, final String key, final Object value) {
    if (null == value)
      span.setAttribute(key, "<NULL>");
    else if (value instanceof Long longValue)
      span.setAttribute(key, longValue);
    else if (value instanceof Double doubleValue)
      span.setAttribute(key, doubleValue);
    else if (value instanceof Integer integerValue)
      span.setAttribute(key, integerValue);
    else if (value instanceof Boolean booleanValue)
      span.setAttribute(key, booleanValue);
    else
      span.setAttribute(key, value.toString());
  }

  private Span startSpan(final MethodSignature signature, final Object target) {
    String operationName = getOperationName(signature, target);
    // NOTE: setParent(...) is not required; `Span.current()` is automatically added as the parent
    return tracer.spanBuilder(operationName).startSpan();
  }

  private String getOperationName(final MethodSignature signature, final Object target) {
    String operationName;
    TraceInvocation newSpanAnnotation = signature.getMethod().getAnnotation(TraceInvocation.class);
    if (!StringUtils.hasText(newSpanAnnotation.operationName())) {
      operationName = (target != null
          ? ClassUtils.getUserClass(target).getSimpleName()
          : signature.getDeclaringType().getSimpleName()) + "." + signature.getName();
    } else {
      operationName = newSpanAnnotation.operationName();
    }

    return operationName;
  }
}
