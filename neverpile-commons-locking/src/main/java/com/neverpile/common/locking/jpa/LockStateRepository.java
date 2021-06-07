package com.neverpile.common.locking.jpa;

import java.time.Instant;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LockStateRepository extends CrudRepository<LockStateEntity, String> {
  @Modifying
  void deleteByValidUntilBefore(Instant before);
}
