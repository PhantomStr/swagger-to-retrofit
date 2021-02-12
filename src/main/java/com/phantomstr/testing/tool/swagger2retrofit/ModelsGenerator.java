package com.phantomstr.testing.tool.swagger2retrofit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import v2.io.swagger.models.Model;
import v2.io.swagger.models.Swagger;
import v2.io.swagger.models.properties.Property;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

import static com.phantomstr.testing.tool.swagger2retrofit.CamelCaseUtils.toCamelCase;
import static java.lang.System.lineSeparator;

@Slf4j
class ModelsGenerator {
    private Reporter reporter = new Reporter("Swagger to RetroFit Models generator report");
    private ClassMapping classMapping;

    void generate(Swagger swagger) {
        reporter.setRowFormat(" - " + Config.targetModelsPackage + ".%s");

        for (Map.Entry<String, Model> modelEntry : swagger.getDefinitions().entrySet()) {
            String modelName = toCamelCase(modelEntry.getKey(), false);
            Map<String, Property> properties = modelEntry.getValue().getProperties();
            makeModel(modelName, properties);
            reporter.append(modelName);
        }
        reporter.print(log::info);
    }

    ModelsGenerator setClassMapping(ClassMapping classMapping) {
        this.classMapping = classMapping;
        return this;
    }

    @SneakyThrows
    private void makeModel(String modelName, Map<String, Property> properties) {
        FileUtils.write(new File(Config.getOutputModelsDirectory() + modelName + ".java"), createModelClass(modelName, properties));
    }

    private String createModelClass(String className, Map<String, Property> properties) {
        return "package " + Config.targetModelsPackage + ";" + lineSeparator() +
                classMapping.getImports() + lineSeparator() + lineSeparator()
                + "@" + Data.class.getSimpleName() + lineSeparator()
                + "@" + With.class.getSimpleName() + lineSeparator()
                + "@" + NoArgsConstructor.class.getSimpleName() + lineSeparator()
                + "@" + AllArgsConstructor.class.getSimpleName() + lineSeparator()
                + "public class " + className + " {" + lineSeparator()
                + properties.entrySet().stream()
                .map(stringPropertyEntry -> "    private " + classMapping.getParameterType(stringPropertyEntry.getValue()) + " " + stringPropertyEntry.getKey() + ";").collect(Collectors.joining(lineSeparator())) +
                lineSeparator()
                + "}" + lineSeparator();
    }

}
