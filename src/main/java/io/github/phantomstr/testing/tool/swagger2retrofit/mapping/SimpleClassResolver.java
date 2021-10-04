package io.github.phantomstr.testing.tool.swagger2retrofit.mapping;

import lombok.SneakyThrows;

import java.util.List;

import static io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetModelsPackage;
import static io.github.phantomstr.testing.tool.swagger2retrofit.utils.CamelCaseUtils.toCamelCase;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

public class SimpleClassResolver {

    public static String getCanonicalTypeName(String type) {
        if (type == null) {
            return "Void";
        }
        if (type.startsWith("#/definitions/")) {
            return getDefinition(type);
        }
        if (type.startsWith("#/components")) {
            return getDefinition(type);
        }
        Class<?> primitive = getPrimitive(type);
        if (primitive == null) {
            return getDefinition(type);
        }
        return primitive.getCanonicalName();
    }

    public static String getSimpleTypeName(String type) {
        return getSimpleNameFromCanonical(getCanonicalTypeName(type));
    }

    public static String getSimpleNameFromCanonical(String canonicalTypeName) {
        return substringAfterLast("." + canonicalTypeName, ".");
    }

    private static String getDefinition(String type) {
        if (type.startsWith("#/definitions/classpath")) {
            return substringAfter(type, "#/definitions/classpath:");
        }
        String shortClassName = toCamelCase(defaultIfEmpty(substringAfterLast(type, "/"), type), false);
        return targetModelsPackage + "." + shortClassName;
    }

    @SneakyThrows
    private static Class<?> getPrimitive(String type) {
        try {
            if (type.equalsIgnoreCase("Array")) {
                return List.class;
            }
            return Class.forName(capitalize(type));
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName("java.lang." + capitalize(type));
            } catch (ClassNotFoundException ex) {
                try {
                    return Class.forName("java.util." + capitalize(type));
                } catch (ClassNotFoundException exception) {
                    return null;
                }
            }
        }
    }

}
