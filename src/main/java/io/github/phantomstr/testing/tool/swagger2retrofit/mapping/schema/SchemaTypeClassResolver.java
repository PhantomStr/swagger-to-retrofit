package io.github.phantomstr.testing.tool.swagger2retrofit.mapping.schema;

import io.swagger.oas.models.media.Schema;
import lombok.AllArgsConstructor;

import static io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver.getCanonicalTypeName;
import static io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver.getSimpleNameFromCanonical;

@AllArgsConstructor
public class SchemaTypeClassResolver {

    public String getSimpleTypeName(Schema<?> schema) {
        String canonicalTypeName;
        if (schema.getType() != null) {
            canonicalTypeName = getCanonicalTypeName(schema.getType());
        } else {
            canonicalTypeName = getCanonicalTypeName(schema.get$ref());
        }
        if (canonicalTypeName.equals("void")) {
            return "Void";
        }

        return getSimpleNameFromCanonical(canonicalTypeName);
    }

}
