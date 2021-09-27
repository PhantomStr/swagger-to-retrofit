package io.github.phantomstr.testing.tool.swagger2retrofit.service.collector;

import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import io.github.phantomstr.testing.tool.swagger2retrofit.service.ServiceClass;
import io.github.phantomstr.testing.tool.swagger2retrofit.service.parser.SwaggerParser;
import v2.io.swagger.models.Swagger;

import java.util.List;
import java.util.Set;

public class SwaggerServicesCollector {


    private final SwaggerParser parser;

    public SwaggerServicesCollector(ClassMapping classMapping, Set<String> requiredModels) {
        parser = new SwaggerParser(classMapping, requiredModels);
    }

    public List<ServiceClass> collectServices(Swagger swagger) {
        return parser.parseServiceMethods(swagger);
    }

}
