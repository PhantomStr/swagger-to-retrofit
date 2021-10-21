package io.github.phantomstr.testing.tool.swagger2retrofit.model;

import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;
import lombok.experimental.Delegate;
import v2.io.swagger.models.properties.Property;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.lang.System.lineSeparator;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

@Getter
@Setter
@With
@AllArgsConstructor
@NoArgsConstructor
public class ModelClass {

    private final Set<Class<?>> modelAnnotations = new HashSet<>();
    String packageName;
    boolean isStatic = false;
    String anExtends;
    Set<String> imports = new HashSet<>();
    Map<String, Property> properties = new HashMap<>();
    Set<ModelClass> innerClasses = new HashSet<>();
    private ModelClass parentModel;
    private String name;

    {
        modelAnnotations.add(AllArgsConstructor.class);
        modelAnnotations.add(Data.class);
        modelAnnotations.add(NoArgsConstructor.class);
        modelAnnotations.add(With.class);
    }

    public CharSequence generate(ClassMapping classMapping) {
        final StringBuilder classSourceCode = new StringBuilder();

        if (parentModel == null) {
            classSourceCode
                    .append("package ").append(packageName).append(";").append(lineSeparator())
                    .append(lineSeparator());
        }


        //class
        StringBuilder classLines = getClassLines(classMapping, this, parentModel == null ? 0 : 4);

        //imports
        StringBuilder importLines = new StringBuilder();
        imports.stream()
                .filter(Objects::nonNull)
                //.filter(importClass -> !importClass.startsWith(targetModelsPackage))
                .filter(s -> !s.startsWith("java.lang") && !s.equals("Void"))
                .sorted()
                .forEach(aClass -> importLines
                        .append("import ").append(aClass).append(";").append(lineSeparator()));
        importLines.append(lineSeparator());

        return classSourceCode.append(importLines).append(classLines).toString();

    }

    private StringBuilder getClassLines(ClassMapping classMapping, ModelClass modelClass, int shift) {
        additionalChecks(modelClass);

        StringBuilder classLines = new StringBuilder();

        modelClass.getModelAnnotations()
                .stream()
                .sorted(Comparator.comparing(Class::getSimpleName))
                .forEach(aClass -> classLines
                        .append(repeat(" ", shift)).append("@").append(aClass.getSimpleName()).append(lineSeparator()));

        classLines
                .append(repeat(" ", shift))
                .append("public")
                .append(isStatic ? " static" : "")
                .append(" class ")
                .append(modelClass.getName())
                .append(anExtends != null ? " extends " + anExtends : "")
                .append(" {")
                .append(lineSeparator())
                .append(lineSeparator());

        if (anExtends != null && !anExtends.contains("<")) {
            classLines
                    .append(repeat(" ", shift))
                    .append("    @Delegate(types = " + anExtends + ".class)").append(lineSeparator())
                    .append(repeat(" ", shift))
                    .append("    @Getter").append(lineSeparator())
                    .append(repeat(" ", shift))
                    .append("    " + anExtends + " " + uncapitalize(anExtends) + ";").append(lineSeparator());
        }

        //fields
        modelClass.getProperties().forEach((fieldName, fieldType) -> {
            if (fieldType.getReadOnly() != null && fieldType.getReadOnly()) {
                classLines.append("// readonly field").append(lineSeparator());
            }
            if (fieldType.getRequired()) {
                classLines.append(repeat(" ", shift)).append("    @NotNull").append(lineSeparator());
                defaultIfNull(modelClass.getParentModel(), modelClass).getImports().add("javax.validation.constraints.NotNull");
            }
            classLines
                    .append(repeat(" ", shift))
                    .append("    private ")
                    .append(classMapping.getSimpleTypeName(fieldType))
                    .append(" ")
                    .append(fieldName)
                    .append(";")
                    .append(lineSeparator());
        });
        innerClasses.forEach(innerClass -> {
            defaultIfNull(getParentModel(), this).getImports().addAll(innerClass.getImports());
            innerClass.setImports(new HashSet<>());
            innerClass.setPackageName(null);
            innerClass.setStatic(true);
            classLines.append(lineSeparator())
                    .append(innerClass.generate(classMapping))
                    .append(lineSeparator());
        });
        classLines.append(lineSeparator()).append(repeat(" ", shift)).append("}").append(lineSeparator());
        return removeDoubleEmptyLines(classLines);
    }

    private StringBuilder removeDoubleEmptyLines(StringBuilder classLines) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean prevLineIsEmpty = false;
        for (String line : classLines.toString().split(lineSeparator())) {
            boolean lineIsEmpty = line.trim().isEmpty();
            if (prevLineIsEmpty && lineIsEmpty) {
                continue;
            }
            prevLineIsEmpty = lineIsEmpty;
            stringBuilder.append(line).append(lineSeparator());
        }

        return stringBuilder;
    }

    private void additionalChecks(ModelClass modelClass) {
        if (modelClass.getProperties().isEmpty() && (anExtends == null || anExtends.contains("<"))) {
            modelClass.getModelAnnotations().remove(AllArgsConstructor.class);
            modelClass.getModelAnnotations().remove(Data.class);
            modelClass.getModelAnnotations().remove(With.class);
            imports.remove(AllArgsConstructor.class.getCanonicalName());
            imports.remove(Data.class.getCanonicalName());
            imports.remove(With.class.getCanonicalName());
        }
        if (anExtends != null && !anExtends.contains("<")) {
            modelClass.getModelAnnotations().add(AllArgsConstructor.class);

            imports.add(AllArgsConstructor.class.getCanonicalName());
            imports.add(Delegate.class.getCanonicalName());
            imports.add(Getter.class.getCanonicalName());
        }

    }

}