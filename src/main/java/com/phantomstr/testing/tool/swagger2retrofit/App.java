package com.phantomstr.testing.tool.swagger2retrofit;

import com.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import com.phantomstr.testing.tool.swagger2retrofit.model.ModelsGenerator;
import com.phantomstr.testing.tool.swagger2retrofit.service.ServiceGenerator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import v2.io.swagger.models.Swagger;
import v2.io.swagger.parser.Swagger20Parser;

import java.io.IOException;
import java.util.Collections;

import static com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetModelsPackage;
import static com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetServicePackage;


public final class App {
    private static final ClassMapping classMapping = new ClassMapping();

    public static void main(String[] args) throws IOException {
        Swagger swagger = new Swagger20Parser().read(readArgs(args), Collections.emptyList());

        new ModelsGenerator()
                .setClassMapping(classMapping)
                .generate(swagger);

        new ServiceGenerator()
                .setClassMapping(classMapping)
                .generate(swagger);

    }

    private static String readArgs(String[] args) {
        Options options = new Options();
        options.addOption(new Option("u", "url", true, "Swagger URL, like http://localhost:8080/v2/api-docs"));
        options.addOption(new Option("mp", "modelsPackage", true, "generated model's package"));
        options.addOption(new Option("sp", "servicePackage", true, "generated service's package"));

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException pe) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("codegen", options);
        }

        assert cmd != null;
        if (cmd.hasOption("mp")) {
            targetModelsPackage = cmd.getOptionValue("mp");
        }
        if (cmd.hasOption("sp")) {
            targetServicePackage = cmd.getOptionValue("sp");
        }
        if (cmd.hasOption("u")) {
            return cmd.getOptionValue("u");
        } else {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("codegen", options);
            throw new RuntimeException("url required!");
        }

    }

}
