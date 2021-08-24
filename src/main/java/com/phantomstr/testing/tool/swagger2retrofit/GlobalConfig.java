package com.phantomstr.testing.tool.swagger2retrofit;

import java.io.File;

public class GlobalConfig {

    public static String targetModelsPackage = "generated.model";
    public static String targetServicePackage = "generated.service";
    public static String targetSchemasDirectory = "model/schema/";
    public static String targetDirectory = "src/main/java";
    public static String resourcesDirectory = "src/main/resources";
    public static String apiRoot = "";
    public static String serviceFilter = "";
    public static String outputDirectory = "/target";

    public static String localRepository = System.getProperty("user.home") + "\\.m2\\repository";

    public static String getOutputServiceDirectory() {
        return getRoot() + targetServicePackage.replace('.', File.separatorChar) + File.separatorChar;
    }

    public static String getOutputModelsDirectory() {
        return getRoot() + targetModelsPackage.replace('.', File.separatorChar) + File.separatorChar;
    }

    public static String getOutputSchemaDirectory() {
        return resourcesDirectory + File.separatorChar + targetSchemasDirectory + File.separatorChar;
    }

    private static String getRoot() {
        return targetDirectory + File.separatorChar;
    }

}
