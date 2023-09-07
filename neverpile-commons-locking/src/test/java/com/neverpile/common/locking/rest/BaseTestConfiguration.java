package com.neverpile.common.locking.rest;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootConfiguration
@EnableWebSecurity
public class BaseTestConfiguration {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    // @formatter:off
    http
        .cors().and()
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeRequests().anyRequest().anonymous()
    ;
    // @formatter:on
    return http.build();
  }
}