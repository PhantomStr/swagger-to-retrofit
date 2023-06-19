package io.github.phantomstr.testing.tool.swagger2retrofit.mapping.apiresponse;

import io.github.phantomstr.testing.tool.swagger2retrofit.Dispatcher;
import io.github.phantomstr.testing.tool.swagger2retrofit.DispatcherImpl;
import io.github.phantomstr.testing.tool.swagger2retrofit.GenericClass;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.responses.ApiResponse;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;

import static io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver.getCanonicalTypeName;

@AllArgsConstructor
public class ApiResponseClassResolver {

    ClassMapping classMapping;

    public Set<String> getTypeNames(ApiResponse parameter) {
        Set<String> types = new HashSet<>();

        Dispatcher dispatcher = new DispatcherImpl();

        dispatcher.addHandler(new GenericClass<>(ApiResponse.class), abstractSerializableParameter -> {

            String $ref = abstractSerializableParameter.get$ref();
            if ($ref != null) {
                types.add(getCanonicalTypeName($ref));
            }
            Content content = parameter.getContent();
            if (content != null) {
                if (content.containsKey("application/json")) {
                    MediaType mediaType = content.get("application/json");
                    Schema schema = mediaType.getSchema();
                    if (schema != null) {
                        String schemaRef = schema.get$ref();
                        if (schemaRef != null) {
                            types.add(getCanonicalTypeName(schemaRef));
                        }
                    }
                }
            }
        });

        dispatcher.handle(parameter);

        return types;
    }

    public String getSimpleTypeName(ApiResponse parameter) {

        String $ref = parameter.get$ref();
        if ($ref != null) {
            return SimpleClassResolver.getSimpleTypeName($ref);
        }
        Content content = parameter.getContent();
        if (content != null) {
            if (content.containsKey("application/json")) {
                MediaType mediaType = content.get("application/json");
                Schema schema = mediaType.getSchema();
                if (schema != null) {
                    return classMapping.getSimpleTypeName(schema);
                }
            }
            return "String";
        }
        return "Void";

    }

}
