package com.neverpile.common.authorization.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.neverpile.common.authorization.policy.Effect;

public class Permission {
  public static Permission allow(final String ...actionKeys) {
    return new Permission(Effect.ALLOW, actionKeys);
  }

  public static Permission deny(final String ...actionKeys) {
    return new Permission(Effect.DENY, actionKeys);
  }
  
  /**
   * The action keys this permission affects. Permission keys may use namespacing as described in {@link Action}. They
   * may also use the trailing wildcard <code>*</code>.
   */
  private List<String> actionKeys = new ArrayList<>();
  
  /**
   * The effect to apply when one of the keys match.
   */
  private Effect effect = Effect.DENY;

  public Permission() {
  }
  
  public Permission(final Effect effect, final String ...actionKeys) {
    super();
    this.actionKeys = Arrays.asList(actionKeys);
    this.effect = effect;
  }
  
  public Permission(final Effect effect, final List<String> actionKeys) {
    super();
    this.actionKeys = actionKeys;
    this.effect = effect;
  }

  public List<String> getActionKeys() {
    return actionKeys;
  }

  public void setActionKeys(final List<String> actionKeys) {
    this.actionKeys = actionKeys;
  }

  public Effect getEffect() {
    return effect;
  }

  public void setEffect(final Effect effect) {
    this.effect = effect;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((actionKeys == null) ? 0 : actionKeys.hashCode());
    result = prime * result + ((effect == null) ? 0 : effect.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Permission other = (Permission) obj;
    if (actionKeys == null) {
      if (other.actionKeys != null)
        return false;
    } else if (!actionKeys.equals(other.actionKeys))
      return false;
    if (effect != other.effect)
      return false;
    return true;
  }

  @Override
  public String toString() {
    return effect + actionKeys.toString();
  }
}
