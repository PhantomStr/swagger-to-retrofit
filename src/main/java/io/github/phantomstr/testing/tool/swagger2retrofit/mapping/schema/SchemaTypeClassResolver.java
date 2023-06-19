package io.github.phantomstr.testing.tool.swagger2retrofit.mapping.schema;

import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.Schema;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver.getCanonicalTypeName;
import static io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver.getSimpleNameFromCanonical;

@AllArgsConstructor
@Slf4j
public class SchemaTypeClassResolver {

    public String getSimpleTypeName(Schema<?> schema) {
        String canonicalTypeName;
        if (schema.getType() != null) {
            if ("array".equals(schema.getType())) {
                Schema items = ((ArraySchema) schema).getItems();
                if (items == null) {
                    log.error("empty items for array schema! \n{}", schema);
                    return "List<Object>";
                }
                return "List<" + getSimpleTypeName(items) + ">";
            }
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
