package com.neverpile.common.opentelemetry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Parameter annotation used on methods annotated with {@link TraceInvocation} used to indicate that the
 * given method parameter should be used as the value of a span attribute.
 */
@Target({
    ElementType.PARAMETER, ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface Attribute {
  class NoopMapper implements Function<Object, Object> {
    @Override
    public Object apply(final Object t) {
      return t; // just a dummy
    }
  }

  /**
   * The name of the attribute for a traced parameter.
   * @return The name of the attribute.
   */
  String name() default "";

  /**
   * An optional implementation of a {@link Function} used to map from the argument value to the attribute
   * value.
   * @return Function to map a non-standard value for tracing.
   */
  Class<? extends Function<? extends Object, ? extends Object>> valueAdapter() default NoopMapper.class;

  interface AttributeExtractor<V> {
    void extract(V value, BiConsumer<String, Object> attributeCreator);
  }
  
  public static class NoopExtractor implements AttributeExtractor<Object> {
    @Override
    public void extract(final Object value, final BiConsumer<String, Object> a) {
      // just a dummy
    }
  }
  
  /**
   * An optional implementation of a {@link Function} used to map from the argument value to the attribute
   * value.
   * @return Function to map a non-standard value for tracing.
   */
  Class<? extends AttributeExtractor<?>> attributeExtractor() default NoopExtractor.class;
}
