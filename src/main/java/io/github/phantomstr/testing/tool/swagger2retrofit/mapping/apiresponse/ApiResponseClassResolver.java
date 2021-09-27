package io.github.phantomstr.testing.tool.swagger2retrofit.mapping.apiresponse;

import io.github.phantomstr.testing.tool.swagger2retrofit.Dispatcher;
import io.github.phantomstr.testing.tool.swagger2retrofit.DispatcherImpl;
import io.github.phantomstr.testing.tool.swagger2retrofit.GenericClass;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver;
import io.swagger.oas.models.responses.ApiResponse;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@AllArgsConstructor
public class ApiResponseClassResolver {

    private final SimpleClassResolver classResolver;

    public Set<String> getTypeNames(ApiResponse parameter) {
        Set<String> types = new HashSet<>();

        Dispatcher dispatcher = new DispatcherImpl();

        dispatcher.addHandler(new GenericClass<>(ApiResponse.class), abstractSerializableParameter -> {
            String type = abstractSerializableParameter.get$ref();
            types.add(classResolver.getCanonicalTypeName(type));
        });

        dispatcher.handle(parameter);

        return types;
    }

    public String getSimpleTypeName(ApiResponse parameter) {
        AtomicReference<String> simpleTypeName = new AtomicReference<>();

        Dispatcher dispatcher = new DispatcherImpl();
        dispatcher.addHandler(new GenericClass<>(ApiResponse.class), bodyParameter -> simpleTypeName.set(bodyParameter.get$ref()));

        dispatcher.handle(parameter);

        return classResolver.getSimpleNameFromCanonical(classResolver.getCanonicalTypeName(simpleTypeName.get()));
    }

}
