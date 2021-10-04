package io.github.phantomstr.testing.tool.swagger2retrofit.model;

import io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import io.github.phantomstr.testing.tool.swagger2retrofit.reader.properties.SchemaPropertiesReader;
import io.github.phantomstr.testing.tool.swagger2retrofit.reader.properties.SchemaPropertiesReader.InnerClassProperty;
import io.github.phantomstr.testing.tool.swagger2retrofit.reporter.Reporter;
import io.swagger.oas.models.Components;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import v2.io.swagger.models.properties.ArrayProperty;
import v2.io.swagger.models.properties.Property;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetModelsPackage;
import static io.github.phantomstr.testing.tool.swagger2retrofit.utils.CamelCaseUtils.toCamelCase;

@SuppressWarnings("rawtypes")
@Slf4j
public class OpenApiModelsGenerator {

    private final List<ModelClass> modelClasses = new ArrayList<>();
    private final Reporter reporter = new Reporter("Swagger to RetroFit Models generator report");
    private final ClassMapping classMapping;
    private final SchemaPropertiesReader schemaPropertiesReader;
    @Setter
    private Set<String> requiredModels;

    public OpenApiModelsGenerator(ClassMapping classMapping) {
        this.classMapping = classMapping;
        schemaPropertiesReader = new SchemaPropertiesReader(classMapping);
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
        loadSchemas(components);
        generateRequestBodies(components);
        generateResponses(components);

        modelClasses.forEach(modelClass -> {
            Set<String> imports = modelClass.getImports();
            imports.addAll(modelClass.getModelAnnotations().stream().map(Class::getCanonicalName).collect(Collectors.toSet()));
            if (modelClass.getAnExtends() != null && modelClass.getAnExtends().contains("ArrayList")) {
                imports.add(ArrayList.class.getCanonicalName());
            }
        });

    }

    private void loadSchemas(@NotNull Components components) {
        Map<String, Schema> schemas = components.getSchemas();
        if (schemas == null) return;
        schemas.forEach((modelName, schema) -> modelClasses.add(createModelClass(components, toCamelCase(modelName, false), schema)));
        addInnerClasses(components, modelClasses);
    }

    private void addInnerClasses(Components components, Collection<ModelClass> modelClasses) {
        for (ModelClass modelClass : modelClasses) {
            for (Property innerClassProperty : modelClass.getProperties().values()) {
                if (innerClassProperty instanceof InnerClassProperty) {
                    addInnerClass(components, modelClass, (InnerClassProperty) innerClassProperty);
                }
                if (innerClassProperty instanceof ArrayProperty) {
                    Property items = ((ArrayProperty) innerClassProperty).getItems();
                    if (items instanceof InnerClassProperty) {
                        addInnerClass(components, modelClass, (InnerClassProperty) items);
                    }
                }
            }
        }
    }

    private void addInnerClass(Components components, ModelClass modelClass, InnerClassProperty innerClassProperty) {
        innerClassProperty.setCanonicalClassName(modelClass.getPackageName() + "."
                                                         + toCamelCase(getRootModel(modelClass).getName(), false) + "." +
                                                         toCamelCase(innerClassProperty.getName(), false));
        Schema<?> classSchema = innerClassProperty.getSchema();
        ModelClass innerModel = createModelClass(components, toCamelCase(innerClassProperty.getName() + "Inner", false), classSchema);

        ModelClass rootModel = getRootModel(modelClass);
        Set<String> rootModelImports = rootModel.getImports();
        innerModel.setParentModel(rootModel);
        //rebind properties to parent model
        innerModel.getProperties().values().stream()
                .filter(property -> property instanceof InnerClassProperty)
                .map(property -> (InnerClassProperty) property)
                .forEach(subClassProperty -> subClassProperty.setCanonicalClassName(rootModel.getPackageName() + "."
                                                                                            + toCamelCase(rootModel.getName(), false) + "." +
                                                                                            toCamelCase(subClassProperty.getName() + "Inner", false)));
        //add imports of inner class into parent model
        //own imports
        rootModelImports.addAll(classMapping.getCanonicalTypeNames(innerModel.getProperties().values()));
        //inner self classes imports
        rootModelImports.addAll(rootModel.getInnerClasses().stream()
                                        .map(inner -> rootModel.getPackageName() + "." + rootModel.getName() + "." + inner.getName())
                                        .collect(Collectors.toSet()));

        rootModel.getInnerClasses().add(innerModel);

        addInnerClasses(components, Collections.singleton(innerModel));

    }

    private ModelClass createModelClass(Components components, String modelName, Schema schema) {
        Map<String, Property> properties = schemaPropertiesReader.readProperties(components, schema, modelName);
        ModelClass modelClass = createModelClass(modelName);
        modelClass.setProperties(properties);
        modelClass.getImports().addAll(classMapping.getCanonicalTypeNames(properties.values()));
        return modelClass;
    }

    private ModelClass getRootModel(ModelClass modelClass) {
        ModelClass parent = modelClass.getParentModel();
        ModelClass root = modelClass;
        while (parent != null) {
            root = parent;
            parent = parent.getParentModel();
        }
        return root;
    }

    private void generateRequestBodies(@NotNull Components components) {
        Map<String, RequestBody> bodies = components.getRequestBodies();
        if (bodies == null) return;
        bodies.forEach((modelName, requestBody) -> modelClasses.add(createModelClass(components, modelName, requestBody.getContent())));
        addInnerClasses(components, modelClasses);
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
        Map<String, ApiResponse> responses = components.getResponses();
        if (responses == null) return;
        responses.forEach((modelName, response) -> modelClasses.add(createModelClass(components, modelName, response.getContent())));
        addInnerClasses(components, modelClasses);
    }

    private ModelClass createModelClass(@NotNull Components components, String modelName, Content content) {
        ModelClass modelClass = createModelClass(toCamelCase(modelName, false));
        Map<String, Property> properties = new HashMap<>();
        content.values().forEach(mediaType -> {
            Schema schema = mediaType.getSchema();
            if (schema != null) {
                if (schema.get$ref() != null) {
                    modelClass.setAnExtends(classMapping.getSimpleTypeName(schema));
                } else {
                    if (schema instanceof ArraySchema) {
                        modelClass.setAnExtends("ArrayList<" + classMapping.getSimpleTypeName(((ArraySchema) schema).getItems()) + ">");
                    } else {
                        Map<String, Property> schemaProperties = schemaPropertiesReader.readProperties(components, schema, modelName);
                        if (schemaProperties != null) {
                            properties.putAll(schemaProperties);
                        }
                    }
                }
            }
        });

        return modelClass
                .withProperties(properties)
                .withImports(classMapping.getCanonicalTypeNames(properties.values()));
    }

    private ModelClass createModelClass(String modelName) {
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
