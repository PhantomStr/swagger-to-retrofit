package com.phantomstr.testing.tool.swagger2retrofit;

import java.io.File;
import java.io.FileNotFoundException;

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


    public static String getOutputServiceDirectory() {
        return getRoot() + targetServicePackage.replace('.', File.separatorChar) + File.separatorChar;
    }

    public static String getOutputModelsDirectory() {
        return getRoot() + targetModelsPackage.replace('.', File.separatorChar) + File.separatorChar;
    }

    public static String getOutputSchemaDirectory() {
        return resourcesDirectory + File.separatorChar + targetSchemasDirectory + File.separatorChar;
    }

    public static String getLombokPath() throws FileNotFoundException {
        String path = localRepository + "\\org\\projectlombok\\lombok\\" + lombokVersion + "\\lombok-" + lombokVersion + ".jar";
        if (!new File(path).exists()) {
            throw new FileNotFoundException("for runtime class compilation required file " + path);
        }
        return path;
    }

    private static String getRoot() {
        return targetDirectory + File.separatorChar;
    }

}
