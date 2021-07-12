package com.neverpile.common.locking;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "neverpile.locking")
@EnableConfigurationProperties
public class LockingConfiguration {
  public static class Jpa {
    /**
     * Whether to enable JPA-based locking
     */
    private boolean enabled;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
  
  /**
   * The duration of the validity of an acquired lock. Default: one minute.
   */
  private Duration validityDuration = Duration.ofMinutes(1);
  
  private Jpa jpa = new Jpa();
  
  public Duration getValidityDuration() {
    return validityDuration;
  }

  public void setValidityDuration(Duration lockValidity) {
    this.validityDuration = lockValidity;
  }

  public Jpa getJpa() {
    return jpa;
  }

  public void setJpa(Jpa jpa) {
    this.jpa = jpa;
  }
}
