package com.neverpile.common.locking.jpa;

import java.time.Instant;

import org.springframework.data.repository.CrudRepository;

/**
 * A repository for {@link LockStateEntity}s.
 */
public interface LockStateRepository extends CrudRepository<LockStateEntity, String> {
  void deleteByValidUntilBefore(Instant before);
}
