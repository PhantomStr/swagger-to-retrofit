package io.github.phantomstr.testing.tool.swagger2retrofit.service.parser;

import io.github.phantomstr.testing.tool.swagger2retrofit.Dispatcher;
import io.github.phantomstr.testing.tool.swagger2retrofit.DispatcherImpl;
import io.github.phantomstr.testing.tool.swagger2retrofit.GenericClass;
import io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import io.github.phantomstr.testing.tool.swagger2retrofit.reporter.Reporter;
import io.github.phantomstr.testing.tool.swagger2retrofit.service.MethodCall;
import io.github.phantomstr.testing.tool.swagger2retrofit.service.ServiceClass;
import io.swagger.oas.models.Components;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.parameters.PathParameter;
import io.swagger.oas.models.parameters.QueryParameter;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.serviceFilter;
import static io.github.phantomstr.testing.tool.swagger2retrofit.mapping.SimpleClassResolver.getCanonicalTypeName;
import static io.github.phantomstr.testing.tool.swagger2retrofit.utils.CamelCaseUtils.toCamelCase;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.stripStart;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

@Slf4j
@Getter
public class OpenApiParser {

    private final List<ServiceClass> serviceClasses = new ArrayList<>();
    private final Reporter reporter = new Reporter("Swagger to RetroFit Service generator report");
    private final ClassMapping classMapping;
    private final Map<String, Parameter> parametersMapping = new HashMap<>();
    private final Set<String> requiredModels;
    private OpenAPI openAPI;

    public OpenApiParser(ClassMapping classMapping, Set<String> requiredModels) {
        this.classMapping = classMapping;
        this.requiredModels = requiredModels;
    }

    public List<ServiceClass> parseServiceMethods(OpenAPI openAPI) {
        this.openAPI = openAPI;
        readComponents(openAPI.getComponents());

        openAPI.getPaths().forEach(this::parseMethods);
        return serviceClasses;
    }

    private void readComponents(Components components) {
        if (components == null) return;
        readParameters(components.getParameters());
    }

    private void readParameters(Map<String, Parameter> parameters) {
        if (parameters == null) return;
        parametersMapping.putAll(parameters);
    }

    private void parseMethods(String key, io.swagger.oas.models.PathItem value) {
        parseGets(key, value.getGet());
        parsePosts(key, value.getPost());
        parsePuts(key, value.getPut());
        parseDeletes(key, value.getDelete());
    }

    private void parseGets(String path, Operation get) {
        if (get != null) {
            addCall(path, get, GET.class.getSimpleName());
        }
    }

    private void parsePosts(String path, Operation post) {
        if (post != null) {
            addCall(path, post, POST.class.getSimpleName());
        }
    }

    private void parsePuts(String path, Operation put) {
        if (put != null) {
            addCall(path, put, PUT.class.getSimpleName());
        }
    }

    private void parseDeletes(String path, Operation delete) {
        if (delete != null) {
            addCall(path, delete, DELETE.class.getSimpleName());
        }
    }

    private void addCall(String path, Operation endpoint, String operation) {
        if (endpoint == null || endpoint.getResponses() == null) {
            reporter.warn("EP " + path + " isn't described or has no responses");
            return;
        }

        String tag = getTag(endpoint);
        if (filteredByTag(path, tag)) return;

        //method generation
        String relativePath = removeStart(stripStart(path, "/"), GlobalConfig.apiRoot);

        ApiResponse property200 = endpoint.getResponses().entrySet().stream()
                .filter(e -> e.getKey().startsWith("2"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        MethodCall methodCall = new MethodCall()
                .withOperation(operation)
                .withPath(relativePath)
                .withMethodName(getMethodName(operation, relativePath))
                .withResponseClass(classMapping.getSimpleTypeName(property200))
                .withParameters(getParameters(endpoint))
                .withAnnotations(getAnnotations());

        // fill services imports
        ServiceClass serviceClass = getServiceClass(toCamelCase(tag + "Service", false));
        Set<String> imports = serviceClass.getImports();
        imports.addAll(classMapping.getCanonicalTypeNames(property200));
        imports.addAll(getParameterImports(endpoint));
        imports.addAll(getResponseImports(property200));


        // fill required models list
        imports.stream()
                .filter(s -> s.startsWith(GlobalConfig.targetModelsPackage))
                .forEach(p -> requiredModels.add(StringUtils.substringAfterLast(p, ".")));

        // append method in service
        serviceClass.getCalls().add(methodCall);
    }

    private Collection<String> getResponseImports(ApiResponse property200) {
        Set<String> imports = new HashSet<>();

        String typeName = classMapping.getSimpleTypeName(property200);
        try {
            //workaround for schemas that just array of reference schema
            Schema schema = openAPI.getComponents().getResponses().get(typeName)
                    .getContent()
                    .get("application/json")
                    .getSchema();
            if (schema instanceof ArraySchema) {
                imports.add(List.class.getCanonicalName());
                imports.add(getCanonicalTypeName(((ArraySchema) schema).getItems().get$ref()));
            }
            return imports;
        } catch (NullPointerException ignore) {}
        imports.add(getCanonicalTypeName(classMapping.getSimpleTypeName(property200)));
        return imports;
    }

    private List<String> getParameters(Operation endpoint) {
        ArrayList<String> parameters = new ArrayList<>();

        Dispatcher dispatcher = new DispatcherImpl();
        dispatcher.addHandler(new GenericClass<>(PathParameter.class), pathParameter -> {
            String name = pathParameter.getName();
            String type = classMapping.getSimpleTypeName(pathParameter.getSchema());
            switch (pathParameter.getIn()) {
                case "path":
                    parameters.add("@Path(\"" + name + "\") " + type + " " + name);
                    break;
                case "query":
                    parameters.add("@Query(\"" + name + "\") " + type + " " + name);
                    break;
                case "header":
                    parameters.add("@Header(\"" + name + "\") " + type + " " + name);
                    break;
                default:
                    throw new RuntimeException("unknown type of path parameter " + name);
            }

        });

        dispatcher.addHandler(new GenericClass<>(QueryParameter.class), parameter -> {
            String type = classMapping.getSimpleTypeName(parameter);
            String name = defaultIfBlank(parameter.getName(), uncapitalize(type));
            parametersMapping.get(name);
            Parameter mappedParameter = parametersMapping.get(name);
            if (mappedParameter != null) {
                type = classMapping.getSimpleTypeName(mappedParameter);
            } else {
                Schema schema = parameter.getSchema();
                if (schema != null) {
                    type = classMapping.getSimpleTypeName(schema);
                }
            }
            parameters.add("@Query(\"" + name + "\") " + type + " " + name);
        });

        dispatcher.addHandler(new GenericClass<>(Parameter.class), parameter -> {
            String type = classMapping.getSimpleTypeName(parameter);
            String name = defaultIfBlank(parameter.getName(), uncapitalize(type));
            parametersMapping.get(name);
            Parameter mappedParameter = parametersMapping.get(name);
            type = classMapping.getSimpleTypeName(mappedParameter);
            switch (mappedParameter.getIn()) {
                case "path":
                    parameters.add("@Path(\"" + name + "\") " + type + " " + name);
                    break;
                case "query":
                    parameters.add("@Query(\"" + name + "\") " + type + " " + name);
                    break;
                case "header":
                    parameters.add("@Header(\"" + name + "\") " + type + " " + name);
                    break;
                default:
                    throw new RuntimeException("unknown type of path parameter " + name);
            }
        });

        if (endpoint.getParameters() != null) {
            endpoint.getParameters().forEach(dispatcher::handle);
        }
        RequestBody body = endpoint.getRequestBody();
        if (body != null) {
            String type = classMapping.getSimpleTypeName(body);
            String name = uncapitalize(type);
            parameters.add("@Body " + type + " " + name);

        }
        return parameters;
    }

    private Collection<String> getParameterImports(Operation endpoint) {
        HashSet<String> imports = new HashSet<>();

        Dispatcher dispatcher = new DispatcherImpl();
        dispatcher.addHandler(new GenericClass<>(PathParameter.class), pathParameter -> imports.addAll(classMapping.getCanonicalTypeNames(pathParameter)));
        dispatcher.addHandler(new GenericClass<>(QueryParameter.class), queryParameter -> imports.addAll(classMapping.getCanonicalTypeNames(queryParameter)));
        dispatcher.addHandler(new GenericClass<>(Parameter.class), parameter -> {
            Set<String> canonicalTypeNames = classMapping.getCanonicalTypeNames(parameter);
            canonicalTypeNames.forEach(s -> {
                Parameter ref = parametersMapping.get(uncapitalize(defaultIfEmpty(substringAfterLast(s, "."), s)));
                if (ref != null) {
                    imports.addAll(classMapping.getCanonicalTypeNames(ref));
                } else {
                    imports.add(s);
                }
            });
        });

        List<Parameter> parameters = endpoint.getParameters();
        if (parameters != null) {
            parameters.forEach(dispatcher::handle);
        }
        parametersMapping.values().forEach(dispatcher::handle);

        if (endpoint.getRequestBody() != null) {
            imports.addAll(classMapping.getCanonicalTypeNames(endpoint.getRequestBody()));
        }

        imports.remove("Void");
        return imports;
    }

    private String getTag(Operation endpoint) {
        String tag;
        if (endpoint == null || endpoint.getTags() == null) {
            tag = "default";
        } else {
            tag = endpoint.getTags().stream().findFirst().orElse("default");
        }
        return tag;
    }

    private boolean filteredByTag(String path, String tag) {
        if (!serviceFilter.isEmpty()) {
            try {
                Pattern pattern = Pattern.compile(serviceFilter);
                if (!pattern.matcher(tag).find()) {
                    log.debug("method " + path + " ignored");
                    return true;
                }
            } catch (Exception e) {
                log.error("can't parse pattern " + serviceFilter, e);
            }
        }
        return false;
    }

    private String getMethodName(String operation, String relativePath) {
        return toCamelCase(operation.toLowerCase() + " " + relativePath.replaceAll("\\\\/\\{}", " "), true);
    }

    private List<Class<?>> getAnnotations() {
        return new ArrayList<>();
    }

    private ServiceClass getServiceClass(String className) {
        ServiceClass service;
        Predicate<ServiceClass> byName = serviceClass -> className.equals(serviceClass.getName());
        if (serviceClasses.stream().noneMatch(byName)) {
            service = new ServiceClass();
            service.setName(className);
            serviceClasses.add(service);
        } else {
            service = serviceClasses.stream().filter(byName).findFirst()
                    .orElseThrow(() -> new RuntimeException("can't find service class " + className));
        }
        return service;
    }

}