package com.phantomstr.testing.tool.swagger2retrofit;

import java.io.File;
import java.io.FileNotFoundException;

import static java.lang.String.format;

public class GlobalConfig {

    public static String targetModelsPackage = "generated.model";
    public static String targetServicePackage = "generated.service";
    public static String targetSchemasDirectory = "model/schema/";
    public static String targetDirectory = "src/main/java";
    public static String resourcesDirectory = "src/main/resources";
    public static String apiRoot = "";
    public static String serviceFilter = "";
    public static Boolean generateSchemas = false;
    public static String outputDirectory = "/target";

    public static String localRepository = System.getProperty("user.home") + "\\.m2\\repository";
    public static String lombokVersion = "1.18.20";
    public static String validationVersion = "1.1.0.Final";


    public static String getOutputServiceDirectory() {
        return getRoot() + targetServicePackage.replace('.', File.separatorChar) + File.separatorChar;
    }

    public static String getOutputModelsDirectory() {
        return getRoot() + targetModelsPackage.replace('.', File.separatorChar) + File.separatorChar;
    }

    public static String getOutputSchemaDirectory() {
        return resourcesDirectory + File.separatorChar + targetSchemasDirectory + File.separatorChar;
    }

    public static String getDependencies() throws FileNotFoundException {
        String lombokPath = localRepository + "\\org\\projectlombok\\lombok\\" + lombokVersion + "\\lombok-" + lombokVersion + ".jar";

        String validationPath = localRepository + "\\javax\\validation\\validation-api\\" + validationVersion + "\\validation-api-" + validationVersion + ".jar";

        if (!new File(lombokPath).exists()) {
            throw new FileNotFoundException("for runtime class compilation required file " + lombokPath);
        }
        if (!new File(validationPath).exists()) {
            throw new FileNotFoundException("for runtime class compilation required file " + validationPath);
        }
        return format("%s;%s", lombokPath, validationPath);
    }

    private static String getRoot() {
        return targetDirectory + File.separatorChar;
    }

}
