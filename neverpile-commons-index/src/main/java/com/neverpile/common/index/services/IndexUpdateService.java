package com.neverpile.common.index.services;

/**
 * Service to maintain an index. This services provides access to operations like index/delete/update on single
 * objects and updates the index accordingly.
 * Administrative functionality to rebuild or reset the index are also provided.
 */
public interface IndexUpdateService {

  /**
   * Create new index entry for an object
   *
   * @param obj new object to index
   */
  void index(Indexable obj);

  /**
   * Update index information for an object already in the index.
   *
   * @param obj object to update index information of
   */
  void update(Indexable obj);

  /**
   * delete object from the index.
   *
   * @param obj object to delete index information of
   */
  void delete(Indexable obj);

  /**
   * Delete whole Index and reinitialized with current mapping information only.
   * <p>
   * <u>Caution</u>: All index data will be lost!
   */
  // void hardResetIndex();

  /**
   * Rebuild the index from scratch. Information for the rebuild will be pulled directly from the database.
   * Current index will remain unchanged for all incoming requests until process is complete.
   * Updates to the index during the process will be included in the new index but won't be accessible
   * until rebuild is complete.
   */
  // void rebuildIndex();
}
