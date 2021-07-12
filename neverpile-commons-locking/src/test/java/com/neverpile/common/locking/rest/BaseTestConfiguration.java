package com.neverpile.common.locking.rest;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@SpringBootConfiguration
public class BaseTestConfiguration {
  @Bean
  public WebSecurityConfigurerAdapter security() {
    return new WebSecurityConfigurerAdapter() {
      @Override
      public void configure(final HttpSecurity http) throws Exception {
        // @formatter:off
        http
          .cors().and()
          .csrf().disable()
          .authorizeRequests().anyRequest().anonymous()
        ;
        // @formatter:on
      }
    };
  }
}