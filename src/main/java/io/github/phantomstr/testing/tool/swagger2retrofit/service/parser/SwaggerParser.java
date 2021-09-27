package io.github.phantomstr.testing.tool.swagger2retrofit.service.parser;

import io.github.phantomstr.testing.tool.swagger2retrofit.Dispatcher;
import io.github.phantomstr.testing.tool.swagger2retrofit.DispatcherImpl;
import io.github.phantomstr.testing.tool.swagger2retrofit.GenericClass;
import io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig;
import io.github.phantomstr.testing.tool.swagger2retrofit.SafeDispatcherImpl;
import io.github.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import io.github.phantomstr.testing.tool.swagger2retrofit.reporter.Reporter;
import io.github.phantomstr.testing.tool.swagger2retrofit.service.MethodCall;
import io.github.phantomstr.testing.tool.swagger2retrofit.service.ServiceClass;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import org.apache.commons.lang3.StringUtils;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import v2.io.swagger.models.Operation;
import v2.io.swagger.models.Path;
import v2.io.swagger.models.Response;
import v2.io.swagger.models.Swagger;
import v2.io.swagger.models.parameters.BodyParameter;
import v2.io.swagger.models.parameters.FormParameter;
import v2.io.swagger.models.parameters.HeaderParameter;
import v2.io.swagger.models.parameters.PathParameter;
import v2.io.swagger.models.parameters.QueryParameter;
import v2.io.swagger.models.properties.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static io.github.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.serviceFilter;
import static io.github.phantomstr.testing.tool.swagger2retrofit.utils.CamelCaseUtils.toCamelCase;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.stripStart;

@Slf4j
@Getter
public class SwaggerParser {

    private final List<ServiceClass> serviceClasses = new ArrayList<>();
    private final Reporter reporter = new Reporter("Swagger to RetroFit Service generator report");
    private final Set<String> requiredModels;
    private final ClassMapping classMapping;

    public SwaggerParser(ClassMapping classMapping, Set<String> requiredModels) {
        assert classMapping != null;
        this.classMapping = classMapping;
        this.requiredModels = requiredModels;
    }

    public List<ServiceClass> parseServiceMethods(Swagger swagger) {
        swagger.getPaths()
                .forEach((path, operations) -> {
                    parseGets(path, operations);
                    parsePosts(path, operations);
                    parsePuts(path, operations);
                    parseDeletes(path, operations);
                });
        return serviceClasses;
    }

    private void parseGets(String path, Path operations) {
        Operation get = operations.getGet();
        if (get != null) {
            addCall(path, get, GET.class.getSimpleName());

            // method validations
            Dispatcher dispatcher = new SafeDispatcherImpl();
            dispatcher.addHandler(new GenericClass<>(BodyParameter.class), parameter ->
                    reporter.appendWarnRow(format("method \"%s:%s\" of type GET has a body! Read RFC https://tools.ietf.org/html/rfc2616#section-4.3",
                                                  get.getTags().stream().findFirst().orElse("root"),
                                                  path)));

            get.getParameters().forEach(dispatcher::handle);
        }
    }

    private void parsePosts(String path, Path operations) {
        Operation post = operations.getPost();
        if (post != null) {
            addCall(path, post, POST.class.getSimpleName());
        }
    }

    private void parsePuts(String path, Path operations) {
        Operation put = operations.getPut();
        if (put != null) {
            addCall(path, put, PUT.class.getSimpleName());
        }
    }

    private void parseDeletes(String path, Path operations) {
        Operation delete = operations.getDelete();
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

        Property property200 = endpoint.getResponses().entrySet().stream()
                .filter(e -> e.getKey().startsWith("2"))
                .map(Map.Entry::getValue)
                .findFirst()
                .map(Response::getSchema)
                .orElse(null);

        MethodCall methodCall = new MethodCall()
                .withOperation(operation)
                .withPath(relativePath)
                .withMethodName(getMethodName(operation, relativePath))
                .withResponseClass(classMapping.getSimpleTypeName(property200))
                .withParameters(getParameters(endpoint))
                .withAnnotations(getAnnotations(endpoint));

        // fill services imports
        ServiceClass serviceClass = getServiceClass(toCamelCase(tag + "Service", false));
        Set<String> imports = serviceClass.getImports();
        imports.addAll(classMapping.getCanonicalTypeNames(Collections.singletonList(property200)));
        imports.addAll(getParameterImports(endpoint));

        // fill required models list
        imports.stream()
                .filter(s -> s.startsWith(GlobalConfig.targetModelsPackage))
                .forEach(p -> requiredModels.add(StringUtils.substringAfterLast(p, ".")));

        // append method in service
        serviceClass.getCalls().add(methodCall);
    }

    private String getTag(Operation endpoint) {
        String tag;
        if (endpoint == null || endpoint.getTags() == null) {
            tag = "root";
        } else {
            tag = endpoint.getTags().stream().findFirst().orElse("root");
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

    private List<Class<?>> getAnnotations(Operation endpoint) {
        List<Class<?>> annotations = new ArrayList<>();

        Dispatcher dispatcher = new DispatcherImpl();
        dispatcher.addHandler(new GenericClass<>(HeaderParameter.class), parameter -> {});
        dispatcher.addHandler(new GenericClass<>(BodyParameter.class), parameter -> {});
        dispatcher.addHandler(new GenericClass<>(PathParameter.class), parameter -> {});
        dispatcher.addHandler(new GenericClass<>(QueryParameter.class), parameter -> {});
        dispatcher.addHandler(new GenericClass<>(FormParameter.class), parameter -> annotations.add(Multipart.class));

        endpoint.getParameters().forEach(dispatcher::handle);
        return annotations;
    }

    private Collection<? extends String> getParameterImports(Operation endpoint) {
        HashSet<String> imports = new HashSet<>();

        Dispatcher dispatcher = new DispatcherImpl();
        dispatcher.addHandler(new GenericClass<>(HeaderParameter.class), parameter -> imports.addAll(classMapping.getCanonicalTypeNames(parameter)));
        dispatcher.addHandler(new GenericClass<>(BodyParameter.class), parameter -> imports.addAll(classMapping.getCanonicalTypeNames(parameter)));
        dispatcher.addHandler(new GenericClass<>(PathParameter.class), parameter -> imports.addAll(classMapping.getCanonicalTypeNames(parameter)));
        dispatcher.addHandler(new GenericClass<>(QueryParameter.class), parameter -> imports.addAll(classMapping.getCanonicalTypeNames(parameter)));
        dispatcher.addHandler(new GenericClass<>(FormParameter.class), parameter -> {
            imports.add(Multipart.class.getCanonicalName());
            imports.add(MultipartBody.class.getCanonicalName());
        });

        endpoint.getParameters().forEach(dispatcher::handle);
        imports.remove("Void");
        return imports;
    }

    private List<String> getParameters(Operation endpoint) {
        List<String> parameters = new ArrayList<>();

        Dispatcher dispatcher = new DispatcherImpl();
        dispatcher.addHandler(new GenericClass<>(HeaderParameter.class), parameter -> {
            String name = StringUtils.uncapitalize(parameter.getName());
            parameters.add("@Header(\"" + parameter.getName() + "\") " + classMapping.getSimpleTypeName(parameter) + " " + name);
        });
        dispatcher.addHandler(new GenericClass<>(BodyParameter.class), parameter -> {
            String name = StringUtils.uncapitalize(parameter.getName());
            parameters.add("@Body " + classMapping.getSimpleTypeName(parameter) + " " + name);
        });
        dispatcher.addHandler(new GenericClass<>(PathParameter.class), parameter -> {
            String name = StringUtils.uncapitalize(parameter.getName());
            parameters.add("@Path(\"" + parameter.getName() + "\") " + classMapping.getSimpleTypeName(parameter) + " " + name);
        });
        dispatcher.addHandler(new GenericClass<>(QueryParameter.class), parameter -> {
            String name = StringUtils.uncapitalize(parameter.getName());
            parameters.add("@Query(\"" + parameter.getName() + "\") " + classMapping.getSimpleTypeName(parameter) + " " + name);
        });
        dispatcher.addHandler(new GenericClass<>(FormParameter.class), parameter -> {
            String name = StringUtils.uncapitalize(parameter.getName());
            parameters.add("@Query(\"" + parameter.getName() + "\") MultipartBody.Part " + name);
        });

        endpoint.getParameters().forEach(dispatcher::handle);

        return parameters;
    }

    private ServiceClass getServiceClass(@NonNull String className) {
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
