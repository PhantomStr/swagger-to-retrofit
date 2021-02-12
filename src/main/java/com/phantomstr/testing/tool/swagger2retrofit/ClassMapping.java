package com.phantomstr.testing.tool.swagger2retrofit;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.With;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import v2.io.swagger.models.parameters.AbstractSerializableParameter;
import v2.io.swagger.models.parameters.BodyParameter;
import v2.io.swagger.models.parameters.Parameter;
import v2.io.swagger.models.properties.ArrayProperty;
import v2.io.swagger.models.properties.Property;
import v2.io.swagger.models.properties.RefProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.phantomstr.testing.tool.swagger2retrofit.CamelCaseUtils.toCamelCase;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

class ClassMapping {
    @Getter
    Map<String, Class<?>> map = new HashMap<>();
    @Getter
    Map<String, String> modelsMap = new HashMap<>();

    ClassMapping() {
        map.put("uuid", UUID.class);
        map.put("get", GET.class);
        map.put("post", POST.class);
        map.put("put", PUT.class);
        map.put("delete", DELETE.class);
        map.put("call", Call.class);
        map.put("body", Body.class);
        map.put("header", Header.class);
        map.put("path", Path.class);
        map.put("data", Data.class);
        map.put("list", List.class);
        map.put("With", With.class);
        map.put("NoArgsConstructor", NoArgsConstructor.class);
        map.put("AllArgsConstructor", AllArgsConstructor.class);

    }

    String getImports() {
        return getMap().values().stream()
                .map(c -> "import " + c.getName() + ";")
                .collect(Collectors.joining(lineSeparator()));
    }

    String getModelImports() {
        return getModelsMap().values().stream()
                .map(fullClassName -> "import " + fullClassName + ";")
                .collect(Collectors.joining(lineSeparator()));
    }

    String getParameterType(Property property) {
        if (property == null) {
            return Void.class.getName();
        }
        if (property.getType().equals("ref") && property instanceof RefProperty) {
            return getFromClassMapping((property));
        }
        return getFromClassMapping(property);
    }

    String getParameterType(Parameter parameter) {
        String format = null;
        if (parameter instanceof AbstractSerializableParameter) {
            AbstractSerializableParameter<?> serializableParameter = (AbstractSerializableParameter<?>) parameter;
            format = defaultIfEmpty(serializableParameter.getFormat(), serializableParameter.getType());
        }
        if (parameter instanceof BodyParameter) {
            BodyParameter bodyParameter = (BodyParameter) parameter;
            format = bodyParameter.getSchema().getReference();
        }

        return getFromClassMapping(format);
    }

    @SneakyThrows
    private Class<?> get(String format) {
        if (map.containsKey(format)) {
            return map.get(format);
        }
        Class<?> aClass;
        try {
            aClass = Class.forName(capitalize(format));
        } catch (ClassNotFoundException e) {
            try {
                aClass = Class.forName("java.lang." + capitalize(format));
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
                throw new RuntimeException("wtf?");
            }
        }
        map.put(format, aClass);
        return aClass;
    }

    private void registerModel(String shortClassName, String className) {
        modelsMap.put(shortClassName, className);
    }

    private String getFromClassMapping(String type) {
        if (type == null) {
            return "void";
        }
        if (type.startsWith("#/definitions/")) {
            String shortClassName = toCamelCase(substringAfterLast(type, "/"), false);
            String className = Config.targetModelsPackage + "." + shortClassName;
            registerModel(shortClassName, className);
            return className;
        }
        return get(type).getSimpleName();
    }

    private String getFromClassMapping(Property property) {
        String type = property.getType();

        if (type.equals("array")) {
            Property items = ((ArrayProperty) property).getItems();
            return format("List<%s>", getFromClassMapping(items));
        }
        if (type.equals("ref")) {
            type = ((RefProperty) property).get$ref();
        }
        return getFromClassMapping(type);
    }

}
