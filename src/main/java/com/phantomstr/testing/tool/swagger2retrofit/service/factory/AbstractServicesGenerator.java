package com.phantomstr.testing.tool.swagger2retrofit.service.factory;

import com.phantomstr.testing.tool.swagger2retrofit.service.ServiceClass;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;

import static com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.getOutputServiceDirectory;
import static com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetServicePackage;
import static java.lang.System.lineSeparator;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

public class AbstractServicesGenerator {

    @SneakyThrows
    public void generate(List<ServiceClass> serviceClasses) {
        FileUtils.write(new File(getOutputServiceDirectory() + "AbstractServices.java"), createServicesFactory(serviceClasses));
    }

    private String createServicesFactory(List<ServiceClass> serviceClasses) {
        final StringBuilder classSourceCode = new StringBuilder();

        classSourceCode
                .append("package ").append(targetServicePackage).append(";").append(lineSeparator())
                .append(lineSeparator())
                .append("import com.phantomstr.testing.tool.utils.rest.RestServiceProvider;").append(lineSeparator())
                .append("import lombok.Getter;")
                .append(lineSeparator())
                .append(lineSeparator())
                .append("public abstract class AbstractServices {").append(lineSeparator())
                .append(lineSeparator())
                .append("    @Getter").append(lineSeparator())
                .append("    protected static RestServiceProvider serviceProvider;").append(lineSeparator())
                .append(lineSeparator());

        serviceClasses.forEach(service -> classSourceCode
                .append("    public static ").append(service.getName()).append(" ").append(uncapitalize(service.getName())).append("() {").append(lineSeparator())
                .append("        return getServiceProvider().getService(").append(service.getName()).append(".class);").append(lineSeparator())
                .append("    }").append(lineSeparator())
                .append(lineSeparator()));

        classSourceCode.append("}").append(lineSeparator());

        return classSourceCode.toString();
    }

}
