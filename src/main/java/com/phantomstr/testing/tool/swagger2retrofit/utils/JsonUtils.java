package com.phantomstr.testing.tool.swagger2retrofit.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;

public class JsonUtils {

    public static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {

        Iterator<String> fieldNames = updateNode.fieldNames();
        while (fieldNames.hasNext()) {

            String fieldName = fieldNames.next();
            JsonNode jsonNode = mainNode.get(fieldName);
            if (jsonNode != null && jsonNode.isObject()) {
                merge(jsonNode, updateNode.get(fieldName));
            } else {
                if (mainNode instanceof ObjectNode) {
                    JsonNode value = updateNode.get(fieldName);
                    ((ObjectNode) mainNode).put(fieldName, value);
                }
            }
        }
        return mainNode;
    }

}
