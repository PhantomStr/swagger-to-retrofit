package io.github.phantomstr.testing.tool.swagger2retrofit.mapping.requestbody;

import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.RequestBody;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;

import static io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver.getCanonicalTypeName;

@AllArgsConstructor
public class RequestBodyClassResolver {

    ClassMapping classMapping;

    public String getSimpleTypeName(RequestBody requestBody) {
        if (requestBody.get$ref() != null) {
            return SimpleClassResolver.getSimpleTypeName(requestBody.get$ref());
        }
        Content content = requestBody.getContent();
        if (content != null) {
            if (content.containsKey("application/json")) {
                MediaType mediaType = content.get("application/json");
                Schema schema = mediaType.getSchema();
                if (schema != null) {
                    return classMapping.getSimpleTypeName(schema);
                }
            }
            if(content.containsKey("application/x-www-form-urlencoded")){
                MediaType mediaType = content.get("application/x-www-form-urlencoded");
                Schema schema = mediaType.getSchema();
                if (schema != null) {
                    return classMapping.getSimpleTypeName(schema);
                }
            }
        }
        return "Void";
    }

    public Set<String> getTypeNames(RequestBody requestBody) {
        Set<String> types = new HashSet<>();

        String $ref = requestBody.get$ref();
        if ($ref != null) {
            types.add(getCanonicalTypeName($ref));
        }
        Content content = requestBody.getContent();
        if (content != null) {
            if (content.containsKey("application/json")) {
                MediaType mediaType = content.get("application/json");
                Schema schema = mediaType.getSchema();
                if (schema != null) {
                    types.add(getCanonicalTypeName(schema.get$ref()));
                }
            }
        }


        return types;
    }

}
