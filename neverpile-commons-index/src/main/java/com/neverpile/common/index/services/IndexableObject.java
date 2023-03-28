package com.neverpile.common.index.services;

import com.fasterxml.jackson.databind.JsonNode;

public interface IndexableObject {

  public JsonNode getDataToIndex();

  String getId();
}
