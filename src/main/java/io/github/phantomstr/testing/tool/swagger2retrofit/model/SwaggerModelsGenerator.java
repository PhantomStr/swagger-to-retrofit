package io.github.phantomstr.testing.tool.swagger2retrofit.model;

import io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import io.github.phantomstr.testing.tool.swagger2retrofit.reporter.Reporter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import v2.io.swagger.models.Swagger;
import v2.io.swagger.models.properties.Property;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetModelsPackage;
import static io.github.phantomstr.testing.tool.swagger2retrofit.utils.CamelCaseUtils.toCamelCase;

@Slf4j
public class SwaggerModelsGenerator {

    private final List<ModelClass> modelClasses = new ArrayList<>();
    private final Reporter reporter = new Reporter("Swagger to RetroFit Models generator report");
    private final ClassMapping classMapping;
    private Set<String> requiredModels;

    public SwaggerModelsGenerator(ClassMapping classMapping) {
        this.classMapping = classMapping;
    }

    public void generate(Swagger swagger) {
        readModels(swagger);
        filterModelsByRequired();
        generateModels();
    }

    public SwaggerModelsGenerator setRequiredModels(Set<String> requiredModels) {
        this.requiredModels = requiredModels;
        return this;
    }

    private void readModels(Swagger swagger) {
        swagger.getDefinitions().forEach((definitionName, model) -> {
            String modelName = toCamelCase(definitionName, false);
            ModelClass modelClass = getModelClass(modelName);

            Map<String, Property> properties = model.getProperties();
            if (properties != null) {
                modelClass.getImports().addAll(classMapping.getCanonicalTypeNames(properties.values()));
                modelClass.getProperties().putAll(properties);
            }
            modelClasses.add(modelClass);
        });
        modelClasses.forEach(modelClass -> modelClass.getImports()
                .addAll(modelClass.getModelAnnotations().stream().map(Class::getCanonicalName).collect(Collectors.toSet())));
    }

    private void generateModels() {
        modelClasses.forEach(this::writeModel);

        reporter.setRowFormat(targetModelsPackage + ".%s");
        modelClasses.forEach(modelClass -> reporter.info(modelClass.getName()));

        reporter.print(log);
    }

    private void filterModelsByRequired() {
        if (requiredModels != null) {

            boolean goDipper = true;
            while (goDipper) {
                Set<String> addSet = new HashSet<>();
                requiredModels.forEach(model -> modelClasses.stream()
                        .filter(modelClass -> modelClass.getName().equals(model))
                        .findFirst()
                        .ifPresent(modelClass -> addSet.addAll(
                                modelClass.getImports().stream()
                                        .filter(s -> s.startsWith(targetModelsPackage))
                                        .map(s -> StringUtils.substringAfterLast(s, "."))
                                        .collect(Collectors.toSet()))));
                addSet.removeAll(requiredModels);
                requiredModels.addAll(addSet);
                goDipper = !addSet.isEmpty();

            }
            Set<ModelClass> toRemove = modelClasses.stream().filter(modelClass -> !requiredModels.contains(modelClass.getName()))
                    .collect(Collectors.toSet());
            toRemove.forEach(modelClass -> log.info("Model " + modelClass.getName() + " ignored"));
            toRemove.forEach(modelClasses::remove);
        }

    }

    private ModelClass getModelClass(String modelName) {
        ModelClass model;
        Predicate<ModelClass> byName = modelClass -> modelName.equals(modelClass.getName());
        if (modelClasses.stream().noneMatch(byName)) {
            model = new ModelClass()
                    .withName(modelName)
                    .withPackageName(targetModelsPackage);
        } else {
            model = modelClasses.stream().filter(byName).findFirst()
                    .orElseThrow(() -> new RuntimeException("can't find model " + modelName));
        }
        return model;
    }

    @SneakyThrows
    private void writeModel(ModelClass modelClass) {
        FileUtils.write(new File(GlobalConfig.getOutputModelsDirectory() + modelClass.getName() + ".java"), modelClass.generate(classMapping));
    }

}
