package io.github.phantomstr.testing.tool.swagger2retrofit.model;

import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;
import v2.io.swagger.models.properties.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetModelsPackage;
import static java.lang.System.lineSeparator;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

@Getter
@Setter
public class ModelClass {

    private final Collection<Class<?>> modelAnnotations = new ArrayList<>();
    String packageName;
    Set<String> imports = new HashSet<>();
    Map<String, Property> properties = new HashMap<>();
    private String name;

    {
        modelAnnotations.add(AllArgsConstructor.class);
        modelAnnotations.add(Data.class);
        modelAnnotations.add(NoArgsConstructor.class);
        modelAnnotations.add(With.class);
    }

    public CharSequence generate(ClassMapping classMapping) {
        final StringBuilder classSourceCode = new StringBuilder();

        classSourceCode
                .append("package ").append(packageName).append(";").append(lineSeparator())
                .append(lineSeparator());


        //class
        StringBuilder classLines = new StringBuilder();
        modelAnnotations.forEach(aClass -> classLines
                .append("@").append(aClass.getSimpleName()).append(lineSeparator()));

        classLines.append("public class ").append(name).append("{").append(lineSeparator());
        //fields
        properties.forEach((fieldName, fieldType) -> {
            if (fieldType.getRequired()) {
                classLines.append("    @NotNull").append(lineSeparator());
                imports.add("javax.validation.constraints.NotNull");
            }
            classLines
                    .append("    private ")
                    .append(classMapping.getSimpleTypeName(fieldType))
                    .append(" ")
                    .append(fieldName)
                    .append(";")
                    .append(lineSeparator());
        });
        if (properties.isEmpty()) {
            classLines.append("    private String empty;").append(lineSeparator());
        }
        classLines.append("}").append(lineSeparator());

        //imports
        StringBuilder importLines = new StringBuilder();
        modelAnnotations.forEach(cl -> imports.add(cl.getCanonicalName()));
        imports.stream()
                .filter(implort -> !substringBeforeLast(implort, ".").equals(targetModelsPackage))
                .filter(s -> !s.startsWith("java.lang"))
                .forEach(aClass -> importLines
                        .append("import ").append(aClass).append(";").append(lineSeparator()));
        importLines.append(lineSeparator());

        return classSourceCode.append(importLines).append(classLines).toString();

    }

}