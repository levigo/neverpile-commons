package com.neverpile.common.opentracingtest;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TestConfiguration {
  @Bean
  public TestTracer tracer() {
    return new TestTracer();
  }
}
