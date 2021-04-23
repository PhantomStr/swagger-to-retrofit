package com.phantomstr.testing.tool.swagger2retrofit.mapping.parameter;

import com.phantomstr.testing.tool.swagger2retrofit.Dispatcher;
import com.phantomstr.testing.tool.swagger2retrofit.DispatcherImpl;
import com.phantomstr.testing.tool.swagger2retrofit.GenericClass;
import com.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver;
import lombok.AllArgsConstructor;
import v2.io.swagger.models.parameters.AbstractSerializableParameter;
import v2.io.swagger.models.parameters.BodyParameter;
import v2.io.swagger.models.parameters.Parameter;
import v2.io.swagger.models.parameters.PathParameter;
import v2.io.swagger.models.parameters.QueryParameter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

@AllArgsConstructor
public class ParameterClassResolver {

    private final SimpleClassResolver classResolver;

    public Set<String> getTypeNames(Parameter parameter) {
        Set<String> types = new HashSet<>();

        Dispatcher dispatcher = new DispatcherImpl();
        dispatcher.addHandler(new GenericClass<>(QueryParameter.class), queryParameter ->
                types.add(classResolver.getCanonicalTypeName(queryParameter.getType())));

        dispatcher.addHandler(new GenericClass<>(AbstractSerializableParameter.class), abstractSerializableParameter -> {
            String type = defaultIfEmpty(abstractSerializableParameter.getFormat(), abstractSerializableParameter.getType());
            types.add(classResolver.getCanonicalTypeName(type));
        });

        dispatcher.addHandler(new GenericClass<>(BodyParameter.class), bodyParameter -> {
            String type = bodyParameter.getSchema().getReference();
            types.add(classResolver.getCanonicalTypeName(type));
        });

        dispatcher.addHandler(new GenericClass<>(PathParameter.class), pathParameter -> {
            String type = pathParameter.getType();
            types.add(classResolver.getCanonicalTypeName(type));
        });

        dispatcher.handle(parameter);

        return types;
    }

    public String getSimpleTypeName(Parameter parameter) {
        AtomicReference<String> simpleTypeName = new AtomicReference<>();

        Dispatcher dispatcher = new DispatcherImpl();
        dispatcher.addHandler(new GenericClass<>(QueryParameter.class), queryParameter ->
                simpleTypeName.set(classResolver.getSimpleNameFromCanonical(classResolver.getCanonicalTypeName(queryParameter.getType()))));
        dispatcher.addHandler(new GenericClass<>(AbstractSerializableParameter.class), abstractSerializableParameter ->
                simpleTypeName.set(defaultIfEmpty(abstractSerializableParameter.getFormat(), abstractSerializableParameter.getType())));
        dispatcher.addHandler(new GenericClass<>(BodyParameter.class), bodyParameter -> simpleTypeName.set(bodyParameter.getSchema().getReference()));
        dispatcher.addHandler(new GenericClass<>(PathParameter.class), pathParameter -> simpleTypeName.set(pathParameter.getType()));

        dispatcher.handle(parameter);

        return classResolver.getSimpleNameFromCanonical(classResolver.getCanonicalTypeName(simpleTypeName.get()));
    }

}
