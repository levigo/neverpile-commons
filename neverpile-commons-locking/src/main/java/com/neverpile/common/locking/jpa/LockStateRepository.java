package com.neverpile.common.locking.jpa;

import java.time.Instant;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * A repository for {@link LockStateEntity}s.
 */
@Repository
public interface LockStateRepository extends CrudRepository<LockStateEntity, String> {
  void deleteByValidUntilBefore(Instant before);
}
