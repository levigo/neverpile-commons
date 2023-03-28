package com.neverpile.common.index.services.jpa;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.neverpile.common.index.services.IndexUpdateService;
import com.neverpile.common.index.services.IndexableObject;


public class JPAMetadataIndexService implements IndexUpdateService {


  private final IndexRepository indexRepository;

  public JPAMetadataIndexService(IndexRepository indexRepository) {
    this.indexRepository = indexRepository;
  }

  @Override
  public void index(IndexableObject obj) {
    if (obj != null && obj.getDataToIndex() != null) {
      List<IndexEntity> indexableList = flattenJsonToIndexableList(obj.getId(), obj.getDataToIndex());
      indexRepository.saveAll(indexableList);
    }
  }

  @Override
  public void update(IndexableObject obj) {
    // the existing metadata - this will be updated in-place to minimize the mutations JPA has to make
    List<IndexEntity> oldEntities = indexRepository.findByObjectId(obj.getId());

    // new unfiltered new values
    List<IndexEntity> newEntries = flattenJsonToIndexableList(obj.getId(), obj.getDataToIndex());

    // DELETIONS:
    List<IndexEntity> deletions = oldEntities.stream() //
        .filter(oldEntry -> newEntries.stream() //
            .noneMatch(newEntry -> newEntry.getIndexPath().equals(oldEntry.getIndexPath())) //
        ) //
        .collect(Collectors.toList());
    oldEntities.removeAll(deletions);

    // save deletions
    indexRepository.deleteAll(deletions);

    // We need to save those entries that are either added or updated.
    // Start by building a list if entries not matching the previous state exactly.
    List<IndexEntity> toSave = oldEntities.stream() //
        .filter(oldEntry -> !newEntries.contains(oldEntry)) //
        .collect(Collectors.toList());

    // UPDATES:
    toSave.forEach(toSaveEntry -> newEntries.stream() //
        .filter(newEntry -> newEntry.getIndexPath().equals(toSaveEntry.getIndexPath())) //
        .forEach(toSaveEntry::updateValue) //
    );

    // ADDITIONS: handle additions by adding new entries for non-existing paths
    newEntries.stream() //
        .filter(newEntry -> toSave.stream() //
            .noneMatch(toSaveEntry -> toSaveEntry.getIndexPath().equals(newEntry.getIndexPath())) //
        ) //
        .forEach(toSave::add);

    // save additions and updates
    indexRepository.saveAll(toSave);
  }

  @Override
  public void delete(IndexableObject collection) {

  }

  private List<IndexEntity> flattenJsonToIndexableList(String objectId, JsonNode jsonNode) {
    Map<String, String> map = new HashMap<>();
    flattenJsonToMap("", jsonNode, map);

    return map.entrySet().stream() //
        .map(entry -> new IndexEntity(objectId, entry.getKey(), entry.getValue())) //
        .collect(Collectors.toList());
  }

  /**
   * converts a JsonNode to a flat Map with json access key as Map key and the actual json leaf value nodes as
   * Strings in the map values.
   *
   * @param currentPath recursive parameter, holds the current json access path - normally starts as ""
   * @param jsonNode    recursive parameter, json Node to convert
   * @param targetMap   recursive parameter, target map contains the result
   */
  private void flattenJsonToMap(String currentPath, JsonNode jsonNode, Map<String, String> targetMap) {
    if (jsonNode.isObject()) {
      ObjectNode objectNode = (ObjectNode) jsonNode;
      Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
      String pathPrefix = currentPath.isEmpty() ? "" : currentPath + ".";
      while (iter.hasNext()) {
        Map.Entry<String, JsonNode> entry = iter.next();
        flattenJsonToMap(pathPrefix + entry.getKey(), entry.getValue(), targetMap);
      }
    } else if (jsonNode.isArray()) {
      ArrayNode arrayNode = (ArrayNode) jsonNode;
      for (int i = 0; i < arrayNode.size(); i++) {
        flattenJsonToMap(currentPath + "[" + i + "]", arrayNode.get(i), targetMap);
      }
    } else if (jsonNode.isValueNode()) {
      ValueNode valueNode = (ValueNode) jsonNode;
      // TODO: separate DataTypes
      targetMap.put(currentPath, valueNode.asText());
    }
  }
}
