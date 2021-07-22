package com.phantomstr.testing.tool.swagger2retrofit;

import java.io.File;

public class GlobalConfig {
    public static String targetModelsPackage = "generated.model";
    public static String targetServicePackage = "generated.service";
    public static String targetDirectory = "src.main.java";
    public static String apiRoot = "";


    public static String getOutputServiceDirectory() {
        return getRoot() + targetServicePackage.replace('.', File.separatorChar) + File.separatorChar;
    }

    public static String getOutputModelsDirectory() {
        return getRoot() + targetModelsPackage.replace('.', File.separatorChar) + File.separatorChar;
    }

    private static String getRoot() {
        return targetDirectory.replace('.', File.separatorChar) + File.separatorChar;
    }

}
