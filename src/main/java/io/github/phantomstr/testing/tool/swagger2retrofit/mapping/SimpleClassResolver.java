package io.github.phantomstr.testing.tool.swagger2retrofit.mapping;

import lombok.SneakyThrows;

import static io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetModelsPackage;
import static io.github.phantomstr.testing.tool.swagger2retrofit.utils.CamelCaseUtils.toCamelCase;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

public class SimpleClassResolver {

    public String getCanonicalTypeName(String type) {
        if (type == null) {
            return "Void";
        }
        if (type.startsWith("#/definitions/")) {
            return getDefinition(type);
        }

        return getPrimitive(type).getCanonicalName();
    }

    public String getSimpleNameFromCanonical(String canonicalTypeName) {
        return substringAfterLast("." + canonicalTypeName, ".");
    }

    private String getDefinition(String type) {
        if(type.startsWith("#/definitions/classpath")){
            return substringAfter(type,"#/definitions/classpath:");
        }
        String shortClassName = toCamelCase(substringAfterLast(type, "/"), false);
        return targetModelsPackage + "." + shortClassName;
    }

    @SneakyThrows
    private Class<?> getPrimitive(String type) {
        try {
            return Class.forName(capitalize(type));
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName("java.lang." + capitalize(type));
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
