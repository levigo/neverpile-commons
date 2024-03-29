package com.neverpile.common.opentracing.aspect;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.annotation.PostConstruct;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.neverpile.common.opentracing.Tag;
import com.neverpile.common.opentracing.Tag.TagExtractor;
import com.neverpile.common.opentracing.TraceInvocation;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;

/**
 * An aspect used to create opentracing spans for calls to methods annotated with
 * {@link TraceInvocation}.
 */
@Aspect
public class OpentracingAspect {
  private static final Logger LOGGER = LoggerFactory.getLogger(OpentracingAspect.class);

  @Autowired
  Tracer tracer;

  private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

  private final Map<Class<? extends Function<Object, Object>>, Function<Object, Object>> valueAdapterCache = new ConcurrentHashMap<>();

  private final Map<Class<? extends TagExtractor<Object>>, TagExtractor<Object>> tagExtractorCache = new ConcurrentHashMap<>();

  @PostConstruct
  public void logActivation() {
    LOGGER.info("Opentracing Tracer found - tracing of methods annotated with @TraceInvocation enabled");
  }

  @Around("execution (@com.neverpile.common.opentracing.TraceInvocation * *.*(..))")
  public Object newSpanAround(final ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Object[] args = joinPoint.getArgs();

    Span span = startSpan(signature, joinPoint.getThis());

    resolveParameters(signature, args, span);

    try (Scope scope = tracer.scopeManager().activate(span)) {
      Object result = joinPoint.proceed(args);
      return result;
    } catch (Throwable ex) {
      Tags.ERROR.set(span, true);
      Map<String, Object> m = new HashMap<>();
      m.put(Fields.EVENT, "error");
      m.put(Fields.ERROR_OBJECT, ex);
      m.put(Fields.MESSAGE, ex.getMessage());
      span.log(m);
      throw ex;
    } finally {
      span.finish();
    }
  }

  private void resolveParameters(final MethodSignature signature, final Object[] args, final Span span)
      throws Exception {
    Parameter[] parameters = signature.getMethod().getParameters();
    for (int i = 0; i < parameters.length; i++) {
      if (parameters[i].getAnnotation(Tag.class) != null) {
        setupTag(signature, i, parameters[i], args[i], span);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void setupTag(final MethodSignature signature, final int parameterIndex, final Parameter parameter,
      final Object arg, final Span span) throws Exception {
    Tag annotation = parameter.getAnnotation(Tag.class);

    // if we have a value extractor, use it
    if (!annotation.tagExtractor().equals(Tag.NoopExtractor.class)) {
      if (!annotation.valueAdapter().equals(Tag.NoopMapper.class))
        LOGGER.warn("@Tag.tagExtractor and @Tag.valueAdapter are mutually exclusive");
      tagExtractorCache //
          .computeIfAbsent((Class<? extends TagExtractor<Object>>) annotation.tagExtractor(),
              c -> (TagExtractor<Object>) BeanUtils.instantiateClass(c)) //
          .extract(arg, (key, value) -> setTag(span, key, value));

    } else {
      // otherwise build a single tag
      String tagKey = !StringUtils.isEmpty(annotation.name())
          ? annotation.name()
          : findParameterName(signature, parameterIndex);
      Object value = arg;

      // if we have a value mapper, apply it to the value
      if (!annotation.valueAdapter().equals(Tag.NoopMapper.class)) {
        value = valueAdapterCache //
            .computeIfAbsent((Class<? extends Function<Object, Object>>) annotation.valueAdapter(),
                c -> (Function<Object, Object>) BeanUtils.instantiateClass(c)) //
            .apply(value);
      }

      setTag(span, tagKey, value);
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

  private void setTag(final Span span, final String key, final Object value) {
    if (null == value)
      span.setTag(key, "<NULL>");
    else if (value instanceof Number)
      span.setTag(key, (Number) value);
    else if (value instanceof Boolean)
      span.setTag(key, (Boolean) value);
    else
      span.setTag(key, value.toString());
  }

  private Span startSpan(final MethodSignature signature, final Object target) {
    Span parentSpan = tracer.scopeManager().activeSpan();
    String operationName = getOperationName(signature, target);

    return tracer.buildSpan(operationName).asChildOf(parentSpan).start();
  }

  private String getOperationName(final MethodSignature signature, final Object target) {
    String operationName;
    TraceInvocation newSpanAnnotation = signature.getMethod().getAnnotation(TraceInvocation.class);
    if (StringUtils.isEmpty(newSpanAnnotation.operationName())) {
      operationName = (target != null
          ? ClassUtils.getUserClass(target).getSimpleName()
          : signature.getDeclaringType().getSimpleName()) + "." + signature.getName();
    } else {
      operationName = newSpanAnnotation.operationName();
    }

    return operationName;
  }
}
