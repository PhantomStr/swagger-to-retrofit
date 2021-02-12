package com.phantomstr.testing.tool.swagger2retrofit;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import v2.io.swagger.models.Operation;
import v2.io.swagger.models.Path;
import v2.io.swagger.models.Response;
import v2.io.swagger.models.Swagger;
import v2.io.swagger.models.parameters.BodyParameter;
import v2.io.swagger.models.parameters.HeaderParameter;
import v2.io.swagger.models.parameters.Parameter;
import v2.io.swagger.models.parameters.PathParameter;
import v2.io.swagger.models.properties.Property;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.phantomstr.testing.tool.swagger2retrofit.CamelCaseUtils.toCamelCase;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

@Slf4j
class ServiceGenerator {
    private Map<String, StringBuilder> serviceMapping = new HashMap<>();
    private Reporter reporter = new Reporter("Swagger to RetroFit Service generator report");
    private ClassMapping classMapping;

    @SneakyThrows
    void generate(Swagger swagger) {
        reporter.setRowFormat(" - %s(%s)");
        swagger.getPaths().forEach((path, operations) -> {
            parseGets(path, operations);
            parsePosts(path, operations);
            parsePuts(path, operations);
            parseDeletes(path, operations);
        });

        for (Map.Entry<String, StringBuilder> entry : serviceMapping.entrySet()) {
            String service = entry.getKey();
            StringBuilder out = entry.getValue();
            FileUtils.write(new File(Config.getOutputServiceDirectory() + service + ".java"), createServiceClass(service, out));
        }
        reporter.print(log::info);
    }

    ServiceGenerator setClassMapping(ClassMapping classMapping) {
        this.classMapping = classMapping;
        return this;
    }

    private String createServiceClass(String className, StringBuilder out) {
        return "package " + Config.targetServicePackage + ";" + lineSeparator() + lineSeparator() +
                classMapping.getImports() + classMapping.getModelImports() + lineSeparator() + lineSeparator()
                + "public interface " + className + " {" + lineSeparator()
                + out + lineSeparator()
                + "}" + lineSeparator();
    }

    private void parseGets(String path, Path operations) {
        Operation get = operations.getGet();
        if (get != null) {
            String operation = GET.class.getSimpleName();
            addCall(path, get, operation);
        }
    }

    private void parsePosts(String path, Path operations) {
        Operation post = operations.getPost();
        if (post != null) {
            String operation = POST.class.getSimpleName();
            addCall(path, post, operation);
        }
    }

    private void parsePuts(String path, Path operations) {
        Operation put = operations.getPut();
        if (put != null) {
            String operation = PUT.class.getSimpleName();
            addCall(path, put, operation);
        }
    }

    private void parseDeletes(String path, Path operations) {
        Operation delete = operations.getDelete();
        if (delete != null) {
            String operation = DELETE.class.getSimpleName();
            addCall(path, delete, operation);
        }
    }

    private void addCall(String path, Operation endpoint, String operation) {
        String method = toCamelCase(operation.toLowerCase() + " " + path.replaceAll("\\\\/\\{}", " "), true);
        String responseClass = getShortParameterType( endpoint.getResponses().entrySet().stream()
                                                             .filter(e -> e.getKey().startsWith("2"))
                                                             .map(Map.Entry::getValue)
                                                             .findFirst()
                                                             .map(Response::getSchema)
                                                             .orElse(null));
        List<String> parameters = new ArrayList<>();
        endpoint.getParameters().forEach(parameter -> {
            if (parameter instanceof HeaderParameter) {
                String name = StringUtils.uncapitalize(parameter.getName());
                parameters.add("@Header(\"" + parameter.getName() + "\") " + getShortParameterType(parameter) + " " + name);
            } else if (parameter instanceof BodyParameter) {
                String name = StringUtils.uncapitalize(parameter.getName());
                parameters.add("@Body " + getShortParameterType(parameter) + " " + name);
            } else if (parameter instanceof PathParameter) {
                String name = StringUtils.uncapitalize(parameter.getName());
                parameters.add("@Path(\"" + parameter.getName() + "\") " + getShortParameterType(parameter) + " " + name);
            } else {
                throw new RuntimeException("unknown parameter type " + parameter.getClass());
            }
        });
        String tag = endpoint.getTags().stream().findFirst().orElse("root");
        String className = toCamelCase(tag + "Service", false);
        boolean isNew = serviceMapping.putIfAbsent(className, new StringBuilder()) == null;
        if (isNew) {
            reporter.appendRow("Generated calls for " + className + ": ");
        }
        StringBuilder out = serviceMapping.get(className);

        out.append(lineSeparator())
                .append(format("    @%s(\"%s\")", operation, path)).append(lineSeparator())
                .append(format("    Call<%s> %s(%s);", responseClass, method, String.join(", ", parameters))).append(lineSeparator());
        reporter.append(operation, path);
    }

    private String getShortParameterType(Property property) {
        if(classMapping.getParameterType(property).startsWith("List<")){
            return "List<"+substringAfterLast(classMapping.getParameterType(property), ".");
        }
        return substringAfterLast(classMapping.getParameterType(property), ".");
    }

    private String getShortParameterType(Parameter parameter) {
        return StringUtils.defaultIfEmpty(substringAfterLast(classMapping.getParameterType(parameter), "."),
                                          classMapping.getParameterType(parameter));
    }

}
