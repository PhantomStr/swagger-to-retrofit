package com.phantomstr.testing.tool.swagger2retrofit.service;

import com.phantomstr.testing.tool.swagger2retrofit.Dispatcher;
import com.phantomstr.testing.tool.swagger2retrofit.DispatcherImpl;
import com.phantomstr.testing.tool.swagger2retrofit.GenericClass;
import com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig;
import com.phantomstr.testing.tool.swagger2retrofit.SafeDispatcherImpl;
import com.phantomstr.testing.tool.swagger2retrofit.mapping.ClassMapping;
import com.phantomstr.testing.tool.swagger2retrofit.reporter.Reporter;
import com.phantomstr.testing.tool.swagger2retrofit.service.factory.AbstractServicesGenerator;
import lombok.NonNull;
import lombok.SneakyThrows;
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

import static com.phantomstr.testing.tool.swagger2retrofit.GlobalConfig.serviceFilter;
import static com.phantomstr.testing.tool.swagger2retrofit.utils.CamelCaseUtils.toCamelCase;
import static java.lang.String.format;

@Slf4j
public
class ServiceGenerator {

    private final List<ServiceClass> serviceClasses = new ArrayList<>();
    private final Reporter reporter = new Reporter("Swagger to RetroFit Service generator report");
    private ClassMapping classMapping;

    @SneakyThrows
    public void generate(Swagger swagger) {
        addServiceMethods(swagger);
        generateServiceClasses();
        generateAbstractServiceFactory();
        generateReport();

    }

    public ServiceGenerator setClassMapping(ClassMapping classMapping) {
        this.classMapping = classMapping;
        return this;
    }

    private void generateReport() {
        serviceClasses.forEach(serviceClass -> {
            reporter.appendInfoRow("Generated calls for " + serviceClass.getName() + ": ");
            serviceClass.getCalls().forEach(call -> reporter.appendInfoRow("    " + call.getShortDescription()));
        });

        reporter.appendInfoRow("Generated AbstractServices for services:");
        serviceClasses.forEach(serviceClass -> reporter.appendInfoRow("    " + serviceClass.getName()));
        reporter.print(log);
    }

    private void generateServiceClasses() {
        serviceClasses.forEach(ServiceClass::generate);
    }

    private void generateAbstractServiceFactory() {
        new AbstractServicesGenerator().generate(serviceClasses);
    }

    private void addServiceMethods(Swagger swagger) {
        Map<String, Path> paths = swagger.getPaths();
        paths.forEach((path, operations) -> {
            parseGets(path, operations);
            parsePosts(path, operations);
            parsePuts(path, operations);
            parseDeletes(path, operations);
        });
    }

    private void parseGets(String path, Path operations) {
        Operation get = operations.getGet();
        if (get != null) {
            String operation = GET.class.getSimpleName();
            addCall(path, get, operation);

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
        String tag = endpoint.getTags().stream().findFirst().orElse("root");
        if (!serviceFilter.isEmpty()) {
            try {
                Pattern pattern = Pattern.compile(serviceFilter);
                if (!pattern.matcher(tag).find()) {
                    log.debug("method " + path + " ignored");
                    return;
                }
            } catch (Exception e) {
                log.error("can't parse pattern " + serviceFilter, e);
            }
        }

        MethodCall methodCall = new MethodCall();
        String relativePath = StringUtils.stripStart(path, "/");
        relativePath = StringUtils.removeStart(relativePath, GlobalConfig.apiRoot);

        methodCall.setPath(relativePath);
        methodCall.setOperation(operation);


        String className = toCamelCase(tag + "Service", false);

        ServiceClass serviceClass = getServiceClass(className);
        Set<String> imports = serviceClass.getImports();

        String method = toCamelCase(operation.toLowerCase() + " " + relativePath.replaceAll("\\\\/\\{}", " "), true);
        methodCall.setMethod(method);

        Property property200 = endpoint.getResponses().entrySet().stream()
                .filter(e -> e.getKey().startsWith("2"))
                .map(Map.Entry::getValue)
                .findFirst()
                .map(Response::getSchema)
                .orElse(null);
        String responseClass = classMapping.getSimpleTypeName(property200);

        imports.addAll(classMapping.getCanonicalTypeNames(Collections.singletonList(property200)));
        methodCall.setResponseClass(responseClass);

        methodCall.setParameters(getParameters(endpoint));
        methodCall.setAnnotations(getAnnotations(endpoint));
        imports.addAll(getImports(endpoint));

        serviceClass.getCalls().add(methodCall);
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

    private Collection<? extends String> getImports(Operation endpoint) {
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
