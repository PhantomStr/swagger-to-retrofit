package io.github.phantomstr.testing.tool.swagger2retrofit.mapping.schema;

import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver;
import io.swagger.oas.models.media.Schema;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SchemaTypeClassResolver {

    private final SimpleClassResolver classResolver;

    public String getSimpleTypeName(Schema<?> schema) {
        String canonicalTypeName;
        if (schema.getType() != null) {
            canonicalTypeName = classResolver.getCanonicalTypeName(schema.getType());
        } else {
            canonicalTypeName = classResolver.getCanonicalTypeName(schema.get$ref());
        }
        if (canonicalTypeName.equals("void")) {
            return "Void";
        }

        return classResolver.getSimpleNameFromCanonical(canonicalTypeName);
    }

}
