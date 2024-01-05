package com.neverpile.common.opentelemetry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used on methods to indicate that a new OpenTelemetry span should be created around the
 * method invocation. Span attributes can be assigned from method parameters using {@link Attribute}
 */
@Target({
    ElementType.METHOD, ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface TraceInvocation {
  String operationName() default "";
}
