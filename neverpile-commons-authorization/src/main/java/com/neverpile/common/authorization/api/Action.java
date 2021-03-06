package com.neverpile.common.authorization.api;

/**
 * An action specifies the type of interaction with a resource that is the subject of a permission
 * check. Implementations are most likely to be based on enums, like the standard
 * {@link CoreActions}, however, other approaches are possible, too, since the only requirement for
 * an Action is to return a unique key.
 * <p>
 * In order to ensure uniqueness, actions can choose to apply name-spacing of the form
 * <code>namespace:action-name</code>. E.g. all {@link CoreActions} are prefixed with the namespace
 * <code>core:</code>.
 */
public interface Action {
  /**
   * Return the action's unique key. The uniqueness-requirement is scoped to any particular
   * installation, not globally.
   * 
   * @return the unique key
   */
  String key();
  
  /**
   * The action matching all other actions with the key <code>*</code>.
   */
  public static final Action ANY = of("*");
  
  /**
   * Generate an action with a given key.
   * 
   * @param key the key
   * @return the action
   */
  public static Action of(final String key) {
    return new Action() {
      @Override
      public String key() {
        return key;
      }
    };
  }
}
