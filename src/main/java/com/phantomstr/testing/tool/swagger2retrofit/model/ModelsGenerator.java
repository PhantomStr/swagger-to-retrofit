package com.phantomstr.testing.tool.swagger2retrofit.model;

import com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig;
import com.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import com.phantomstr.testing.tool.swagger2retrofit.reporter.Reporter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import v2.io.swagger.models.Swagger;
import v2.io.swagger.models.properties.Property;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetModelsPackage;
import static com.phantomstr.testing.tool.swagger2retrofit.utils.CamelCaseUtils.toCamelCase;

@Slf4j
public class ModelsGenerator {
    private final List<ModelClass> modelClasses = new ArrayList<>();
    private final Reporter reporter = new Reporter("Swagger to RetroFit Models generator report");
    private ClassMapping classMapping;

    public void generate(Swagger swagger) {
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
        modelClasses.forEach(this::writeModel);

        reporter.setRowFormat(" - " + targetModelsPackage + ".%s");
        modelClasses.forEach(modelClass -> reporter.append(modelClass.getName()));
        reporter.print(log::info);
    }

    public ModelsGenerator setClassMapping(ClassMapping classMapping) {
        this.classMapping = classMapping;
        return this;
    }

    private ModelClass getModelClass(String modelName) {
        ModelClass model;
        Predicate<ModelClass> byName = modelClass -> modelName.equals(modelClass.getName());
        if (modelClasses.stream().noneMatch(byName)) {
            model = new ModelClass();
            model.setName(modelName);
            modelClasses.add(model);
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
