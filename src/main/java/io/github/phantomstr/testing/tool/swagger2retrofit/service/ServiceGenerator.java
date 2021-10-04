package io.github.phantomstr.testing.tool.swagger2retrofit.service;

import io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import io.github.phantomstr.testing.tool.swagger2retrofit.reporter.Reporter;
import io.github.phantomstr.testing.tool.swagger2retrofit.service.collector.OpenApiServicesCollector;
import io.github.phantomstr.testing.tool.swagger2retrofit.service.collector.SwaggerServicesCollector;
import io.github.phantomstr.testing.tool.swagger2retrofit.service.factory.AbstractServicesGenerator;
import io.swagger.oas.models.OpenAPI;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import v2.io.swagger.models.Swagger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public
class ServiceGenerator {

    private final List<ServiceClass> serviceClasses = new ArrayList<>();
    private final Reporter reporter = new Reporter("Swagger to RetroFit Service generator report");
    @Getter
    private final Set<String> requiredModels = new HashSet<>();
    private final ClassMapping classMapping;

    public ServiceGenerator(ClassMapping classMapping) {
        this.classMapping = classMapping;
    }

    @SneakyThrows
    public void generate(Object api) {
        if (api instanceof Swagger) {
            SwaggerServicesCollector collector = new SwaggerServicesCollector(classMapping, requiredModels);
            serviceClasses.addAll(collector.collectServices((Swagger) api));
        } else if (api instanceof OpenAPI) {
            OpenApiServicesCollector collector = new OpenApiServicesCollector(classMapping, requiredModels);
            serviceClasses.addAll(collector.collectServices((OpenAPI) api));
        } else {
            throw new RuntimeException("unknown api version");
        }

        serviceClasses.forEach(ServiceClass::generate);
        if (GlobalConfig.generateAbstractService) {
            new AbstractServicesGenerator().generate(serviceClasses);
        }

        generateReport();
    }

    private void generateReport() {
        serviceClasses.forEach(serviceClass -> {
            reporter.appendInfoRow("Generated calls for " + serviceClass.getName() + ": ");
            serviceClass.getCalls().forEach(call -> reporter.appendInfoRow("    " + call.getShortDescription()));
        });
        if (GlobalConfig.generateAbstractService) {
            reporter.appendInfoRow("Generated AbstractServices for services:");
            serviceClasses.forEach(serviceClass -> reporter.appendInfoRow("    " + serviceClass.getName()));
        }
        reporter.print(log);
    }

}
