package io.github.phantomstr.testing.tool.swagger2retrofit.mapping;

import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.apiresponse.ApiResponseClassResolver;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.parameter.ParameterClassResolver;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.property.PropertyClassResolver;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.requestbody.RequestBodyClassResolver;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.schema.SchemaTypeClassResolver;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import v2.io.swagger.models.parameters.Parameter;
import v2.io.swagger.models.properties.Property;

import java.util.Collection;
import java.util.Set;

public class ClassMapping {

    SimpleClassResolver classResolver = new SimpleClassResolver();
    PropertyClassResolver propertyClassResolver = new PropertyClassResolver(classResolver);
    ParameterClassResolver parameterClassResolver = new ParameterClassResolver(classResolver);
    ApiResponseClassResolver apiResponseClassResolver = new ApiResponseClassResolver(classResolver);
    SchemaTypeClassResolver schemaTypeClassResolver = new SchemaTypeClassResolver(classResolver);
    RequestBodyClassResolver requestBodyClassResolver = new RequestBodyClassResolver(classResolver);

    public Set<String> getCanonicalTypeNames(Collection<Property> values) {
        return propertyClassResolver.getTypeNames(values);
    }

    public Set<String> getCanonicalTypeNames(Parameter parameter) {
        return parameterClassResolver.getTypeNames(parameter);
    }

    public Set<String> getCanonicalTypeNames(ApiResponse property) {
        return apiResponseClassResolver.getTypeNames(property);
    }

    public String getSimpleTypeName(Property property) {
        return propertyClassResolver.getSimpleTypeName(property);
    }

    public String getSimpleTypeName(Parameter parameter) {
        return parameterClassResolver.getSimpleTypeName(parameter);
    }

    public String getSimpleTypeName(ApiResponse parameter) {
        return apiResponseClassResolver.getSimpleTypeName(parameter);
    }

    public Set<String> getCanonicalTypeNames(io.swagger.oas.models.parameters.Parameter parameter) {
        return parameterClassResolver.getTypeNames(parameter);
    }

    public String getSimpleTypeName(Schema<?> schema) {
        return schemaTypeClassResolver.getSimpleTypeName(schema);
    }

    public String getSimpleTypeName(RequestBody requestBody) {
        return requestBodyClassResolver.getSimpleTypeName(requestBody);
    }

    public Set<String> getCanonicalTypeNames(RequestBody requestBody) {
        return requestBodyClassResolver.getTypeNames(requestBody);
    }

    public String getSimpleTypeName(io.swagger.oas.models.parameters.Parameter parameter) {
        return parameterClassResolver.getSimpleTypeName(parameter);
    }

}
