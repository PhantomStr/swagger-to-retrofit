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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import v2.io.swagger.models.Swagger;
import v2.io.swagger.parser.Swagger20Parser;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import static com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.apiRoot;
import static com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.generateSchemas;
import static com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.serviceFilter;
import static com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetModelsPackage;
import static com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetSchemasDirectory;
import static com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetServicePackage;


public final class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(Swagger20Parser.class);
    private static final ClassMapping classMapping = new ClassMapping();

    public static void main(String[] args) throws IOException {
        Swagger swagger = new Swagger20Parser().read(readArgs(args), Collections.emptyList());
        if (swagger == null) {
            throw new RuntimeException("Can't get swagger. Please check the swagger url");
        }
        ServiceGenerator serviceGenerator = new ServiceGenerator().setClassMapping(classMapping);
        serviceGenerator.generate(swagger);

        Set<String> requiredModels = null;
        if (!serviceFilter.isEmpty()) {
            requiredModels = serviceGenerator.getRequiredModels();
        }

        new ModelsGenerator()
                .setClassMapping(classMapping)
                .setRequiredModels(requiredModels)
                .generate(swagger);

    }

    private static String readArgs(String[] args) {
        Options options = new Options();

        options.addOption(new Option("u", "url", true, "Swagger URL, like http://localhost:8080/v2/api-docs"));

        options.addOption(new Option("mp", "modelsPackage", true, "generated model's package"));

        options.addOption(new Option("sp", "servicePackage", true, "generated service's package"));

        Option apiRootOption = new Option("ar", "apiRoot", true, "generated service's package");
        apiRootOption.setRequired(false);
        options.addOption(apiRootOption);

        Option filterServices = new Option("sf", "serviceFilters", true, "regexp filter of generated services");
        filterServices.setRequired(false);
        options.addOption(filterServices);

        Option generateSchemasFlag = new Option("gs", "generateSchemas", false, "generate schemas");
        generateSchemasFlag.setRequired(false);
        options.addOption(generateSchemasFlag);

        Option generateSchemasDirectory = new Option("gsd", "generateSchemasDirectory", true, "resources directory for schemas");
        generateSchemasDirectory.setRequired(false);
        options.addOption(generateSchemasDirectory);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException pe) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("codegen", options);
            pe.printStackTrace();
        }

        assert cmd != null;
        if (cmd.hasOption("mp")) {
            LOGGER.info("modelsPackage " + cmd.getOptionValue("mp"));
            targetModelsPackage = cmd.getOptionValue("mp");
        }
        if (cmd.hasOption("sp")) {
            LOGGER.info("servicePackage " + cmd.getOptionValue("sp"));
            targetServicePackage = cmd.getOptionValue("sp");
        }
        if (cmd.hasOption("ar")) {
            LOGGER.info("apiRoot " + cmd.getOptionValue("ar"));
            apiRoot = StringUtils.stripStart(cmd.getOptionValue("ar"), "/");
        }
        if (cmd.hasOption("sf")) {
            LOGGER.info("services filter template: " + cmd.getOptionValue("sf"));
            serviceFilter = cmd.getOptionValue("sf");
        }
        if (cmd.hasOption("gs")) {
            if (cmd.hasOption("gsd")) {
                targetSchemasDirectory = cmd.getOptionValue("gsd");
            }
            LOGGER.info("generate schemas into directory " + targetSchemasDirectory);
            generateSchemas = true;
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
