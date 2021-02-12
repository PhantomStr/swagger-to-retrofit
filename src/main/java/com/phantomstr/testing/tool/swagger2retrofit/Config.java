package com.phantomstr.testing.tool.swagger2retrofit;

import java.io.File;

class Config {
    static String targetModelsPackage = "generated.model";
    static String targetServicePackage = "generated.service";

    static String getOutputServiceDirectory() {
        return getRoot() + targetServicePackage.replace('.', File.separatorChar) + File.separatorChar;
    }

    static String getOutputModelsDirectory() {
        return getRoot() + targetModelsPackage.replace('.', File.separatorChar) + File.separatorChar;
    }

    private static String getRoot() {
        return String.join(String.valueOf(File.separatorChar), "src", "main", "java") + File.separatorChar;
    }

}
