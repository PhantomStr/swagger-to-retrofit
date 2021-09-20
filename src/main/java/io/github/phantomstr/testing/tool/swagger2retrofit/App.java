package io.github.phantomstr.testing.tool.swagger2retrofit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import io.github.phantomstr.testing.tool.swagger2retrofit.model.ModelsGenerator;
import io.github.phantomstr.testing.tool.swagger2retrofit.service.ServiceGenerator;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import v2.io.swagger.models.Swagger;
import v2.io.swagger.parser.Swagger20Parser;
import v2.io.swagger.util.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Set;

import static io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.apiRoot;
import static io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.overrideFile;
import static io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.serviceFilter;
import static io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetModelsPackage;
import static io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.targetServicePackage;
import static io.github.phantomstr.testing.tool.swagger2retrofit.utils.JsonUtils.merge;
import static io.github.phantomstr.testing.tool.swagger2retrofit.utils.SSLCertificate.disableSSLVerifier;


public final class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(Swagger20Parser.class);
    private static final ClassMapping classMapping = new ClassMapping();

    public static void main(String[] args) throws IOException {
        try {
            disableSSLVerifier();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOGGER.error("can't disable SSL");
        }

        String url = readArgs(args).getOptionValue("u");
        Swagger swagger;
        if (!overrideFile.isEmpty() && new File(GlobalConfig.overrideFile).exists()) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode o1 = mapper.readValue(new URL(url), ObjectNode.class);
            ObjectNode o2 = mapper.readValue(new File(GlobalConfig.overrideFile), ObjectNode.class);
            JsonNode merge = merge(o1, o2);
            swagger = new Swagger20Parser().read(merge);
        } else {
            swagger = new Swagger20Parser().read(url, Collections.emptyList());
        }
        OutputStream out = new ByteArrayOutputStream();
        Yaml.pretty().writeValue(out, swagger);
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

    private static CommandLine readArgs(String[] args) {
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

        CommandLineParser parser = new BasicParser();
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

        if (cmd.hasOption("u")) {
            return cmd;
        } else {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("codegen", options);
            throw new RuntimeException("url required!");
        }

    }

}
