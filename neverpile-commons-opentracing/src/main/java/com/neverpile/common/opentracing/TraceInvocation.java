package com.neverpile.common.opentracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used on methods to indicate that a new opentracing span should be created around the
 * method invocation. Span tags can be assigned from method parameters using {@link Tag}
 *
 * @deprecated as of 1.14.0, in favor of OpenTelemetry and
 *             {@link com.neverpile.common.opentelemetry.TraceInvocation}
 */
@Deprecated(
    since = "1.14.0")
@Target({
    ElementType.METHOD, ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface TraceInvocation {
  String operationName() default "";
}
