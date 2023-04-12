package com.neverpile.common.index.services;

import com.fasterxml.jackson.databind.JsonNode;

public interface IndexDataExtractor {
  JsonNode indexData(Indexable indexable);
}
