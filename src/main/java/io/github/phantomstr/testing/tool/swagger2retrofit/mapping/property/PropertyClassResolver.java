package io.github.phantomstr.testing.tool.swagger2retrofit.mapping.property;

import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver;
import lombok.AllArgsConstructor;
import v2.io.swagger.models.properties.ArrayProperty;
import v2.io.swagger.models.properties.Property;
import v2.io.swagger.models.properties.RefProperty;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

@AllArgsConstructor
public class PropertyClassResolver {

    private final SimpleClassResolver classResolver;

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

        if (type.equals("array")) {
            Property items = ((ArrayProperty) property).getItems();
            if (items.getType().equals("ref")) {
                type = ((RefProperty) items).get$ref();
                return format("List<%s>", classResolver.getSimpleNameFromCanonical(classResolver.getCanonicalTypeName(type)));
            }
            return format("List<%s>", classResolver.getSimpleNameFromCanonical(classResolver.getCanonicalTypeName(items.getType())));
        }
        if (type.equals("integer")) {
            if ("int64".equals(property.getFormat())) {
                type = "long";
            } else {
                type = "integer";
            }
        }
        if (type.equals("ref")) {
            type = ((RefProperty) property).get$ref();
        }
        String canonicalTypeName = classResolver.getCanonicalTypeName(type);
        if (canonicalTypeName.equals("void")) {
            return "Void";
        }
        return classResolver.getSimpleNameFromCanonical(canonicalTypeName);
    }

    private Set<String> getTypes(Property property) {
        if (property == null) {
            return new HashSet<>();
        }
        String type = property.getType();

        if (type.equals("array")) {
            Property items = ((ArrayProperty) property).getItems();
            Set<String> imports = new HashSet<>();
            imports.add(List.class.getCanonicalName());
            if (items.getType().equals("ref")) {
                type = ((RefProperty) items).get$ref();
                imports.add(List.class.getCanonicalName());
                imports.add(classResolver.getCanonicalTypeName(type));
                return imports;
            }
            imports.add(classResolver.getCanonicalTypeName(items.getType()));
            return imports;
        }
        if (type.equals("ref")) {
            type = ((RefProperty) property).get$ref();
        }
        if (type.equals("integer")) {
            if ("int64".equals(property.getFormat())) {
                type = "long";
            } else {
                type = "integer";
            }
        }
        String canonicalTypeName = classResolver.getCanonicalTypeName(type);
        if (canonicalTypeName.equals("void")) {
            return new HashSet<>();
        }

        Set<String> imports = new HashSet<>();
        imports.add(canonicalTypeName);
        return imports;
    }

}
