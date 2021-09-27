package io.github.phantomstr.testing.tool.swagger2retrofit.mapping.requestbody;

import io.github.phantomstr.testing.tool.swagger2retrofit.Dispatcher;
import io.github.phantomstr.testing.tool.swagger2retrofit.DispatcherImpl;
import io.github.phantomstr.testing.tool.swagger2retrofit.GenericClass;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver;
import io.swagger.oas.models.parameters.RequestBody;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
public class RequestBodyClassResolver {

    private final SimpleClassResolver classResolver;

    public String getSimpleTypeName(RequestBody requestBody) {
        String canonicalTypeName = null;
        if (requestBody.get$ref() != null) {
            canonicalTypeName = classResolver.getCanonicalTypeName(requestBody.get$ref());
        }
        if ("void".equals(canonicalTypeName)) {
            return "Void";
        }

        return classResolver.getSimpleNameFromCanonical(canonicalTypeName);
    }

    public Set<String> getTypeNames(RequestBody requestBody) {
        Set<String> types = new HashSet<>();

        Dispatcher dispatcher = new DispatcherImpl();

        dispatcher.addHandler(new GenericClass<>(RequestBody.class), body -> {
            String type = body.get$ref();
            types.add(classResolver.getCanonicalTypeName(type));
        });

        dispatcher.handle(requestBody);

        return types;
    }

}
