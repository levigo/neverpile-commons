package com.neverpile.common.index.services.jpa;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface IndexRepository extends CrudRepository<IndexEntity, IdAndIndexPath> {

  // TODO: paging (/projections)

  void deleteByObjectId(String collectionId);

  List<IndexEntity> findByObjectId(String collectionId);
}
