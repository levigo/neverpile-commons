package com.neverpile.common.opentracing.aspect;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import io.opentracing.Tracer;

@Configuration
// Configure as late as possible, after any tracer has been created
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnBean(Tracer.class)
public class OpentracingAspectAutoConfiguration {
  @Bean
  public OpentracingAspect opentracingAspect() {
    return new OpentracingAspect();
  }
}
