package com.phantomstr.testing.tool.swagger2retrofit;


import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

import static com.phantomstr.testing.tool.swagger2retrofit.App.main;

@Mojo(name = "generate",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class MavenGenerateGoal extends AbstractMojo {

    @Getter
    @Parameter(property = "com.phantomstr.testing.tool.swagger2retrofit.swagger2retrofit.commandline", required = true)
    private String commandline;

    @Getter
    @Parameter(property = "com.phantomstr.testing.tool.swagger2retrofit.swagger2retrofit.targetDir",
            defaultValue = "${project.build.sourceDirectory}")
    private String targetDir;

    @Getter
    @Parameter(property = "com.phantomstr.testing.tool.swagger2retrofit.swagger2retrofit.overrideFile",
            defaultValue = "${project.basedir}/src/main/resources/s2rOverrides.json")
    private String overrideFile;

    @Getter
    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private String outputDirectory;

    @Getter
    @Parameter(defaultValue = "${settings.localRepository}")
    private String localRepository;

    @SneakyThrows
    @Override
    public void execute() {
        getLog().info("commandline = " + commandline);
        String[] args = commandline.split("\\s");
        GlobalConfig.targetDirectory = targetDir;
        if (outputDirectory != null) {
            GlobalConfig.outputDirectory = outputDirectory;
        }
        if (localRepository != null) {
            GlobalConfig.localRepository = localRepository;
        }
        getLog().info("targetDir = " + targetDir);

        if (overrideFile != null && new File(overrideFile).exists()) {
            GlobalConfig.overrideFile = overrideFile;
            getLog().info("overrideFile = " + overrideFile);
        }
        main(args);
    }

}
