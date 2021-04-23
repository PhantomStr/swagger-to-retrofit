package com.phantomstr.testing.tool.swagger2retrofit;


import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import static com.phantomstr.testing.tool.swagger2retrofit.App.main;

@Mojo(name = "generate",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class MavenGenerateGoal extends AbstractMojo {

    @Getter
    @Parameter(property = "com.phantomstr.testing.tool.swagger2retrofit.swagger2retrofit.commandline", required = true)
    private String commandline;

    @Getter
    @Parameter(property = "com.phantomstr.testing.tool.swagger2retrofit.swagger2retrofit.targetDir", defaultValue = "${project.build.sourceDirectory}")
    private String targetDir;

    @SneakyThrows
    @Override
    public void execute() {
        getLog().info("commandline=" + commandline);
        String[] args = commandline.split("\\s");
        GlobalConfig.targetDirectory = targetDir;
        getLog().info("targetDir=" + targetDir);
        main(args);
    }

}
