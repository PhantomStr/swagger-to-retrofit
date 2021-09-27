package io.github.phantomstr.testing.tool.swagger2retrofit.model;

import io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import io.github.phantomstr.testing.tool.swagger2retrofit.reporter.Reporter;
import io.swagger.oas.models.Components;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.media.ComposedSchema;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import v2.io.swagger.models.properties.Property;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetModelsPackage;
import static io.github.phantomstr.testing.tool.swagger2retrofit.utils.SchemaToPropertyConverter.convert;
import static org.apache.commons.lang3.StringUtils.stripStart;
import static org.apache.commons.lang3.StringUtils.substringBetween;

@SuppressWarnings("rawtypes")
@Slf4j
public class OpenApiModelsGenerator {

    private final List<ModelClass> modelClasses = new ArrayList<>();
    private final Reporter reporter = new Reporter("Swagger to RetroFit Models generator report");
    private final ClassMapping classMapping;
    @Setter
    private Set<String> requiredModels;

    public OpenApiModelsGenerator(ClassMapping classMapping) {
        this.classMapping = classMapping;
    }

    public void generate(OpenAPI openAPI) {
        readModels(openAPI.getComponents());
        filterModelsByRequired();
        generateModels();
    }

    private void readModels(Components components) {
        if (components == null) {
            return;
        }
        generateSchemas(components);
        generateRequestBodies(components);
        generateResponses(components);

    }

    private void generateSchemas(@NotNull Components components) {
        Map<String, Schema> schemas = components.getSchemas();
        if (schemas == null) return;

        schemas.forEach((modelName, schema) -> {
            Map<String, Property> properties = getProperties(components, schema);

            ModelClass model = getModelClass(modelName)
                    .withProperties(properties)
                    .withImports(classMapping.getCanonicalTypeNames(properties.values()));

            modelClasses.add(model);
        });
    }

    private Map<String, Property> getProperties(Components components, Schema schema) {
        Map<String, Property> properties = new HashMap<>();

        //noinspection unchecked
        ((Map<String, Schema>) schema.getProperties())
                .forEach((propName, propSchema) -> {
                    boolean required = schema.getRequired() != null && schema.getRequired().contains(propName);
                    Property property = convert(propSchema, required);
                    properties.put(propName, property);
                });

        //add properties from other schemas if needed
        if (schema instanceof ComposedSchema) {
            List<Schema> allOf = ((ComposedSchema) schema).getAllOf();
            if (allOf != null) {
                allOf.stream()
                        .map(Schema::get$ref)
                        .forEach(readRefProperties(components, properties));
            }
        }
        return properties;
    }

    private Consumer<String> readRefProperties(Components components, Map<String, Property> properties) {
        return ref -> {
            String subSchemaName = substringBetween(ref, "#/components/", "/");
            switch (subSchemaName) {
                case "schemas":
                    Schema subSchema = components.getSchemas().get(stripStart(ref, "#/components/schemas"));
                    properties.putAll(getProperties(components, subSchema));
                    break;
                case "requestBodies":
                case "responses":
                case "parameters":
                default:
                    throw new RuntimeException("can't process reference for" + ref);
            }
        };
    }

    private void generateRequestBodies(@NotNull Components components) {
        Map<String, Property> properties = new HashMap<>();
        Map<String, RequestBody> bodies = components.getRequestBodies();
        if (bodies == null) return;
        bodies.forEach((modelName, requestBody) -> {
            ModelClass model = getModelClass(components, properties, modelName, requestBody.getContent());
            modelClasses.add(model);
        });
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

    @SneakyThrows
    private void writeModel(ModelClass modelClass) {
        FileUtils.write(new File(GlobalConfig.getOutputModelsDirectory() + modelClass.getName() + ".java"), modelClass.generate(classMapping));
    }

    private void generateResponses(@NotNull Components components) {
        Map<String, Property> properties = new HashMap<>();
        Map<String, ApiResponse> responses = components.getResponses();
        if (responses == null) return;
        responses.forEach((modelName, response) -> {
            ModelClass model = getModelClass(components, properties, modelName, response.getContent());
            modelClasses.add(model);
        });
    }

    private ModelClass getModelClass(@NotNull Components components, Map<String, Property> properties, String modelName, Content content) {
        content.values().forEach(mediaType -> {
            Schema schema = mediaType.getSchema();
            if (schema != null) {
                //here should be complex schema processing...

                Map<String, Schema<?>> schemaProperties = schema.getProperties();
                if (schemaProperties != null) {
                    schemaProperties.forEach((propName, propSchema) -> {
                        boolean required = schema.getRequired() != null && schema.getRequired().contains(propName);
                        Property property = convert(propSchema, required);
                        properties.put(propName, property);
                    });
                }
                if (schema.get$ref() != null) {
                    readRefProperties(components, properties).accept(schema.get$ref());
                }

            }
        });
        return getModelClass(modelName)
                .withProperties(properties)
                .withImports(classMapping.getCanonicalTypeNames(properties.values()));
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

}
