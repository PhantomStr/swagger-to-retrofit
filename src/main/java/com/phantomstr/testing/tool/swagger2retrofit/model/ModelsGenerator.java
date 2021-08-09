package com.phantomstr.testing.tool.swagger2retrofit.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.customProperties.ValidationSchemaFactoryWrapper;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig;
import com.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import com.phantomstr.testing.tool.swagger2retrofit.reporter.Reporter;
import com.phantomstr.testing.tool.swagger2retrofit.schema.GenerateSchemas;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import v2.io.swagger.models.Swagger;
import v2.io.swagger.models.properties.Property;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.generateSchemas;
import static com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetModelsPackage;
import static com.phantomstr.testing.tool.swagger2retrofit.utils.CamelCaseUtils.toCamelCase;

@Slf4j
public class ModelsGenerator {

    private final List<ModelClass> modelClasses = new ArrayList<>();
    private final Reporter reporter = new Reporter("Swagger to RetroFit Models generator report");
    private ClassMapping classMapping;
    private Set<String> requiredModels;

    public void generate(Swagger swagger) {
        swagger.getDefinitions().forEach((definitionName, model) -> {
            String modelName = toCamelCase(definitionName, false);
            ModelClass modelClass = getModelClass(modelName);
            modelClass.setPackageName(targetModelsPackage);

            Map<String, Property> properties = model.getProperties();
            if (properties != null) {
                modelClass.getImports().addAll(classMapping.getCanonicalTypeNames(properties.values()));
                modelClass.getProperties().putAll(properties);
            }
            modelClasses.add(modelClass);
        });

        //left only required models for generated services
        filterModelsByRequired();

        modelClasses.forEach(this::writeModel);

        reporter.setRowFormat(targetModelsPackage + ".%s");
        modelClasses.forEach(modelClass -> reporter.info(modelClass.getName()));
        if (generateSchemas) {
            reporter.setRowFormat("schema generation: %s");
            generateSchemas();
        }
        reporter.print(log);
    }

    public ModelsGenerator setClassMapping(ClassMapping classMapping) {
        this.classMapping = classMapping;
        return this;
    }

    public ModelsGenerator setRequiredModels(Set<String> requiredModels) {
        this.requiredModels = requiredModels;
        return this;
    }

    @SneakyThrows
    private void generateSchemas() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> compUnits = fileManager
                    .getJavaFileObjects(modelClasses.stream()
                                                .map(modelClass -> new File(GlobalConfig.getOutputModelsDirectory(), modelClass.getName() + ".java"))
                                                .toArray(File[]::new));

            String generatedResourcesPath = getClassPath().toString();
            Iterable<String> options = Arrays.asList("-cp", GlobalConfig.getLombokPath(), "-d", generatedResourcesPath);
            compiler.getTask(null, fileManager, null, options, null, compUnits).call();
        } catch (FileNotFoundException e) {
            reporter.warn(e);
            return;
        }


        modelClasses.forEach(modelClass -> {
            Class<?> generatedModelClass;
            try {
                generatedModelClass = loadClass(modelClass);
            } catch (ClassNotFoundException e) {
                reporter.warn("cant initialize class " + modelClass.getName());
                return;
            }
            SchemaFactoryWrapper validatorVisitor = new ValidationSchemaFactoryWrapper();
            try {
                JsonSchema jsonSchema = GenerateSchemas.generateSchema(validatorVisitor, generatedModelClass);
                writeSchema(jsonSchema, modelClass);
            } catch (JsonProcessingException e) {
                reporter.warn("can't generate scheme for class " + generatedModelClass, e);

            }
        });
    }

    private Path getClassPath() {
        return Paths.get(new File(GlobalConfig.outputDirectory).getParentFile().getPath(), "generated-sources", "tmp-cls");
    }

    private void writeSchema(JsonSchema jsonSchema, ModelClass modelClass) {
        try {
            String jsonSchemaStr = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema);
            FileUtils.write(new File(GlobalConfig.getOutputSchemaDirectory() + modelClass.getName() + ".json"), jsonSchemaStr);
        } catch (IOException e) {
            reporter.warn("can't write scheme for class " + modelClass, e);
        }
    }

    @SneakyThrows
    private Class<?> loadClass(ModelClass modelClass) throws ClassNotFoundException {
        URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{getClassPath().toUri().toURL()}, getClass().getClassLoader());

        return urlClassLoader.loadClass(modelClass.getPackageName() + "." + modelClass.getName());
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
            model = new ModelClass();
            model.setName(modelName);
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
