package com.neverpile.common.locking.rest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.neverpile.common.openapi.OpenApiFragment;
import com.neverpile.common.openapi.ResourceOpenApiFragment;

/**
 * A configuration exposing the OpenAPI spec for the fusion REST API.
 */
@Configuration
public class LockingServiceOpenApiConfiguration {
  @Configuration
  @ConditionalOnBean(LockServiceResource.class)
  public static class LockingResourceConfiguration {
    @Bean
    public OpenApiFragment lockingOpenApiFragment() {
      return new ResourceOpenApiFragment("neverpile", "locking",
          new ClassPathResource("com/neverpile/common/locking/openapi.yaml"));
    }
  }
}
