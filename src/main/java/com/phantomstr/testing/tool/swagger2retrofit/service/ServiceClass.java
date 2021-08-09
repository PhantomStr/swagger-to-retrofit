package com.phantomstr.testing.tool.swagger2retrofit.service;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.getOutputServiceDirectory;
import static com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetServicePackage;
import static java.lang.System.lineSeparator;

@Getter
public class ServiceClass {

    @Setter
    String packageName;
    @Setter
    String name;
    List<MethodCall> calls = new ArrayList<>();
    Set<String> imports = new HashSet<>();

    {
        imports.add("retrofit2.http.*");
        imports.add("retrofit2.Call");
    }

    @SneakyThrows
    public void generate() {
        FileUtils.write(new File(getOutputServiceDirectory() + name + ".java"), createServiceInterface(calls));
    }


    private String createServiceInterface(List<MethodCall> methodServices) {
        final StringBuilder classSourceCode = new StringBuilder();

        classSourceCode
                .append("package ").append(targetServicePackage).append(";").append(lineSeparator())
                .append(lineSeparator());

        imports.stream()
                .filter(s -> !s.startsWith("java.lang"))
                .sorted()
                .forEach(aClass -> classSourceCode
                        .append("import ").append(aClass).append(";").append(lineSeparator()));

        classSourceCode.append(lineSeparator());

        classSourceCode.append("public interface ").append(name).append("{").append(lineSeparator());

        methodServices.forEach(methodCall -> classSourceCode.append(methodCall).append(lineSeparator()));

        classSourceCode.append("}").append(lineSeparator());

        return classSourceCode.toString();
    }

}
