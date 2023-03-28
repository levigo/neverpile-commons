package com.neverpile.common.index.services;

import com.neverpile.common.condition.Condition;

public interface IndexQuery {

  Condition condition = null;

  Condition getCondition();

  void setCondition();
}
