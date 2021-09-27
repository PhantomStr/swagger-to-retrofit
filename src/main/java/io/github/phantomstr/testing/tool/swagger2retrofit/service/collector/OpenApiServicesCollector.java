package io.github.phantomstr.testing.tool.swagger2retrofit.service.collector;

import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import io.github.phantomstr.testing.tool.swagger2retrofit.service.ServiceClass;
import io.github.phantomstr.testing.tool.swagger2retrofit.service.parser.OpenApiParser;
import io.swagger.oas.models.OpenAPI;

import java.util.List;
import java.util.Set;

public class OpenApiServicesCollector {


    private final OpenApiParser parser;

    public OpenApiServicesCollector(ClassMapping classMapping, Set<String> requiredModels) {
        parser = new OpenApiParser(classMapping, requiredModels);
    }

    public List<ServiceClass> collectServices(OpenAPI openAPI) {
        return parser.parseServiceMethods(openAPI);
    }

}
