package com.neverpile.common.opentracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Parameter annotation used on methods annotated with {@link TraceInvocation} used to indicate that
 * the given method parameter should be used as the value of a span tag.
 *
 * @deprecated as of 1.14.0, in favor of OpenTelemetry and
 *             {@link com.neverpile.common.opentelemetry.Attribute}
 */
@Deprecated(
    since = "1.14.0")
@Target({
    ElementType.PARAMETER, ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface Tag {
  public static class NoopMapper implements Function<Object, Object> {
    @Override
    public Object apply(final Object t) {
      return t; // just a dummy
    }
  }

  /**
   * The name of the tag for a traced parameter.
   * 
   * @return The name of the tag.
   */
  String name() default "";

  /**
   * An optional implementation of a {@link Function} used to map from the argument value to the tag
   * value.
   * 
   * @return Function to map a non standard value for tracing.
   */
  Class<? extends Function<? extends Object, ? extends Object>> valueAdapter() default NoopMapper.class;

  public interface TagExtractor<V> {
    public void extract(V value, BiConsumer<String, Object> tagCreator);
  }

  public static class NoopExtractor implements TagExtractor<Object> {
    @Override
    public void extract(final Object value, final BiConsumer<String, Object> t) {
      // just a dummy
    }
  }

  /**
   * An optional implementation of a {@link Function} used to map from the argument value to the tag
   * value.
   * 
   * @return Function to map a non standard value for tracing.
   */
  Class<? extends TagExtractor<?>> tagExtractor() default NoopExtractor.class;
}
