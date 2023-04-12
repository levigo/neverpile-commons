package com.neverpile.common.index.services;

import java.util.List;

import com.neverpile.common.condition.Condition;


/**
 * Service to run queries against the index. These {@link IndexQuery}s allow the use of complex combinations of
 * {@link Condition}s and filters to return a List of matching {@link Indexable}s.
 */
public interface IndexQueryService {

  /**
   * Uses a Query search for a list of all matching object registered in the index. The list may be empty if no object
   * is matching.
   * 
   * @param searchQuery the query to use
   * @return List of matching documents
   */
  List<Indexable> query(IndexQuery searchQuery);
}
