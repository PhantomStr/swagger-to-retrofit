package com.phantomstr.testing.tool.swagger2retrofit;


import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import static com.phantomstr.testing.tool.swagger2retrofit.App.main;

@Mojo(name = "generate",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class MavenGenerateGoal implements org.apache.maven.plugin.Mojo {
    private static final Log logger = new LogAdapter();

    @Getter
    @Parameter(property = "swagger2retrofit.commandline", required = true)
    private String commandline;

    @SneakyThrows
    @Override
    public void execute() {
        logger.info("commandline=" + commandline);
        String[] args = commandline.split("\\s");
        main(args);
    }

    @Override
    public void setLog(Log log) {
        logger.debug("Logger not configurable with setLog method. Use logback.xml instead.");
    }

    @Override
    public Log getLog() {
        return logger;
    }

}
