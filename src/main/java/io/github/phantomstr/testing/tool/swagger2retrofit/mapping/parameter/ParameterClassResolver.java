package io.github.phantomstr.testing.tool.swagger2retrofit.mapping.parameter;

import io.github.phantomstr.testing.tool.swagger2retrofit.Dispatcher;
import io.github.phantomstr.testing.tool.swagger2retrofit.DispatcherImpl;
import io.github.phantomstr.testing.tool.swagger2retrofit.GenericClass;
import lombok.AllArgsConstructor;
import v2.io.swagger.models.parameters.AbstractSerializableParameter;
import v2.io.swagger.models.parameters.BodyParameter;
import v2.io.swagger.models.parameters.Parameter;
import v2.io.swagger.models.parameters.PathParameter;
import v2.io.swagger.models.parameters.QueryParameter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver.getCanonicalTypeName;
import static io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver.getSimpleNameFromCanonical;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

@AllArgsConstructor
public class ParameterClassResolver {

    public Set<String> getTypeNames(Parameter parameter) {
        Set<String> types = new HashSet<>();

        Dispatcher dispatcher = new DispatcherImpl();
        dispatcher.addHandler(new GenericClass<>(QueryParameter.class), queryParameter ->
                types.add(getCanonicalTypeName(queryParameter.getType())));

        dispatcher.addHandler(new GenericClass<>(AbstractSerializableParameter.class), abstractSerializableParameter -> {
            String type = defaultIfEmpty(abstractSerializableParameter.getFormat(), abstractSerializableParameter.getType());
            types.add(getCanonicalTypeName(type));
        });

        dispatcher.addHandler(new GenericClass<>(BodyParameter.class), bodyParameter -> {
            String type = bodyParameter.getSchema().getReference();
            types.add(getCanonicalTypeName(type));
        });

        dispatcher.addHandler(new GenericClass<>(PathParameter.class), pathParameter -> {
            String type = pathParameter.getType();
            types.add(getCanonicalTypeName(type));
        });

        dispatcher.handle(parameter);

        return types;
    }

    public String getSimpleTypeName(Parameter parameter) {
        AtomicReference<String> simpleTypeName = new AtomicReference<>();

        Dispatcher dispatcher = new DispatcherImpl();
        dispatcher.addHandler(new GenericClass<>(QueryParameter.class), queryParameter ->
                simpleTypeName.set(getSimpleNameFromCanonical(getCanonicalTypeName(queryParameter.getType()))));
        dispatcher.addHandler(new GenericClass<>(AbstractSerializableParameter.class), abstractSerializableParameter ->
                simpleTypeName.set(defaultIfEmpty(abstractSerializableParameter.getFormat(), abstractSerializableParameter.getType())));
        dispatcher.addHandler(new GenericClass<>(BodyParameter.class), bodyParameter -> simpleTypeName.set(bodyParameter.getSchema().getReference()));
        dispatcher.addHandler(new GenericClass<>(PathParameter.class), pathParameter -> simpleTypeName.set(pathParameter.getType()));

        dispatcher.handle(parameter);

        return getSimpleNameFromCanonical(getCanonicalTypeName(simpleTypeName.get()));
    }

    public Set<String> getTypeNames(io.swagger.oas.models.parameters.Parameter parameter) {
        Set<String> types = new HashSet<>();

        Dispatcher dispatcher = new DispatcherImpl();

        dispatcher.addHandler(new GenericClass<>(io.swagger.oas.models.parameters.PathParameter.class), pathParameter ->
                types.add(getCanonicalTypeName(pathParameter.getSchema().getType())));
        dispatcher.addHandler(new GenericClass<>(io.swagger.oas.models.parameters.QueryParameter.class), queryParameter ->
                types.add(getCanonicalTypeName(queryParameter.getSchema().getType())));
        dispatcher.addHandler(new GenericClass<>(io.swagger.oas.models.parameters.Parameter.class), refParameter ->
                types.add(getCanonicalTypeName(refParameter.get$ref())));
        dispatcher.addHandler(new GenericClass<>(io.swagger.oas.models.parameters.HeaderParameter.class), refParameter ->
                types.add(getCanonicalTypeName(refParameter.getSchema().getType())));

        dispatcher.handle(parameter);

        return types;
    }


    //io.swagger.oas.models.parameters.Parameter
    public String getSimpleTypeName(io.swagger.oas.models.parameters.Parameter parameter) {

        String simpleTypeName;

        if (parameter.getSchema() != null) {
            simpleTypeName = defaultIfEmpty(parameter.getSchema().getName(), parameter.getSchema().getType());
        } else if (parameter.get$ref() != null) {
            simpleTypeName = parameter.get$ref();
        } else {
            throw new RuntimeException("can't recognize type by parameter " + parameter);
        }

        return getSimpleNameFromCanonical(getCanonicalTypeName(simpleTypeName));
    }

}
