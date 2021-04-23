package com.phantomstr.testing.tool.swagger2retrofit.mapping;

import com.phantomstr.testing.tool.swagger2retrofit.mapping.parameter.ParameterClassResolver;
import com.phantomstr.testing.tool.swagger2retrofit.mapping.property.PropertyClassResolver;
import v2.io.swagger.models.parameters.Parameter;
import v2.io.swagger.models.properties.Property;

import java.util.Collection;
import java.util.Set;

public class ClassMapping {

    SimpleClassResolver classResolver = new SimpleClassResolver();
    PropertyClassResolver propertyClassResolver = new PropertyClassResolver(classResolver);
    ParameterClassResolver parameterClassResolver = new ParameterClassResolver(classResolver);

    public Set<String> getCanonicalTypeNames(Collection<Property> values) {
        return propertyClassResolver.getTypeNames(values);
    }

    public Set<String> getCanonicalTypeNames(Parameter parameter) {
        return parameterClassResolver.getTypeNames(parameter);
    }

    public String getSimpleTypeName(Property property) {
        return propertyClassResolver.getSimpleTypeName(property);
    }

    public String getSimpleTypeName(Parameter parameter) {
        return parameterClassResolver.getSimpleTypeName(parameter);
    }

}
