package io.github.phantomstr.testing.tool.swagger2retrofit.mapping.property;

import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver;
import io.github.phantomstr.testing.tool.swagger2retrofit.reader.properties.SchemaPropertiesReader.InnerClassProperty;
import lombok.AllArgsConstructor;
import v2.io.swagger.models.properties.ArrayProperty;
import v2.io.swagger.models.properties.Property;
import v2.io.swagger.models.properties.RefProperty;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver.getCanonicalTypeName;
import static io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver.getSimpleNameFromCanonical;
import static io.github.phantomstr.testing.tool.swagger2retrofit.utils.CamelCaseUtils.toCamelCase;
import static java.lang.String.format;

@AllArgsConstructor
public class PropertyClassResolver {

    public Set<String> getTypeNames(Collection<Property> values) {
        Set<String> types = new HashSet<>();
        values.forEach(property -> types.addAll(getTypes(property)));
        return types;
    }

    public String getSimpleTypeName(Property property) {
        if (property == null) {
            return "Void";
        }
        String type = property.getType();

        if ("array".equals(type)) {
            Property itemProperty = ((ArrayProperty) property).getItems();
            if (itemProperty.getType().equals("ref")) {
                type = ((RefProperty) itemProperty).get$ref();
                return format("List<%s>", SimpleClassResolver.getSimpleTypeName(type));
            }
            if ("inner".equals(itemProperty.getType())) {
                this.getSimpleTypeName(itemProperty);
            }
            return format("List<%s>", getSimpleTypeName(itemProperty));
        }
        if ("integer".equals(type)) {
            if ("int64".equals(property.getFormat())) {
                type = "long";
            } else {
                type = "integer";
            }
        }
        if ("inner".equals(type)) {
            return toCamelCase(property.getName() + "Inner", false);
        }
        if ("ref".equals(type)) {
            type = ((RefProperty) property).get$ref();
        }
        String canonicalTypeName = getCanonicalTypeName(type);
        if (canonicalTypeName.equals("void")) {
            return "Void";
        }
        return getSimpleNameFromCanonical(canonicalTypeName);
    }

    private Set<String> getTypes(Property property) {
        if (property == null) {
            return new HashSet<>();
        }
        Set<String> imports = new HashSet<>();
        String type = property.getType();

        if ("array".equals(type)) {
            Property items = ((ArrayProperty) property).getItems();
            imports = new HashSet<>();
            imports.add(List.class.getCanonicalName());
            if (items.getType().equals("ref")) {
                type = ((RefProperty) items).get$ref();
                imports.add(List.class.getCanonicalName());
                imports.add(getCanonicalTypeName(type));
                return imports;
            }
            imports.addAll(getTypes(items));
            return imports;
        }
        if ("ref".equals(type)) {
            type = ((RefProperty) property).get$ref();
        }
        if ("integer".equals(type)) {
            if ("int64".equals(property.getFormat())) {
                type = "long";
            } else {
                type = "integer";
            }
        }
        if ("inner".equals(type)) {
            imports.add(((InnerClassProperty) property).getCanonicalClassName());
            return imports;
        }
        String canonicalTypeName = getCanonicalTypeName(type);
        if (canonicalTypeName.equals("void")) {
            return imports;
        }


        imports.add(canonicalTypeName);
        return imports;
    }

}
