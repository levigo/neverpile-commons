package com.neverpile.authorization.policy;

/**
 * An enum listing the possible outcomes of a authorization check.
 */
public enum Effect {
  /**
   * Allow the operation  
   */
  ALLOW,
  
  /**
   * Deny the operation
   */
  DENY
}
