package com.neverpile.common.index.services;

import com.neverpile.common.condition.Condition;

public interface IndexQuery {
  Condition getCondition();

  void setCondition();
}
