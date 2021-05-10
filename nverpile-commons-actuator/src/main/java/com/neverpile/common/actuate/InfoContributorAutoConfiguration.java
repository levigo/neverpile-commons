package com.neverpile.common.actuate;

import org.springframework.boot.actuate.autoconfigure.info.ConditionalOnEnabledInfoContributor;
import org.springframework.boot.actuate.autoconfigure.info.InfoContributorProperties;
import org.springframework.boot.actuate.info.GitInfoContributor;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Auto-configuration for info contributors.
 */
@Configuration(
    proxyBeanMethods = false)
@AutoConfigureAfter(ProjectInfoAutoConfiguration.class)
@EnableConfigurationProperties(InfoContributorProperties.class)
public class InfoContributorAutoConfiguration {
  /**
   * The default order for the core {@link InfoContributor} beans. ones.
   * 
   * Priority is 5 higher than spring's built-in one
   */
  public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 5;

  @Bean
  @ConditionalOnEnabledInfoContributor("git")
  @ConditionalOnSingleCandidate(GitProperties.class)
  @ConditionalOnMissingBean
  @Order(DEFAULT_ORDER)
  public GitInfoContributor gitInfoContributor(GitProperties gitProperties,
      InfoContributorProperties infoContributorProperties) {
    return new ExtendedGitInfoContributor(gitProperties, infoContributorProperties.getGit().getMode());
  }
}