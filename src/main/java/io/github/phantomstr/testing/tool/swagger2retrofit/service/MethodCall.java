package io.github.phantomstr.testing.tool.swagger2retrofit.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

@AllArgsConstructor
@NoArgsConstructor
@With
@Getter
public class MethodCall {

    List<Class<?>> annotations = new ArrayList<>();
    private String operation, responseClass, methodName, path;
    private List<String> parameters;

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        parameters.sort((o1, o2) -> new ParamOrderComparator().compare(o1, o2));
        if (!annotations.isEmpty()) {
            stringBuilder.append(annotations.stream()
                                         .map(Class::getSimpleName)
                                         .map("    @"::concat)
                                         .collect(joining(lineSeparator())))
                    .append(lineSeparator());
        }
        stringBuilder.append(format("    @%s(\"%s\")", operation, path)).append(lineSeparator())
                .append(format("    Call<%s> %s(%s);", responseClass, methodName, String.join(", ", parameters))).append(lineSeparator());
        return stringBuilder.toString();
    }

    public String getShortDescription() {
        return format("%s %s ", operation, path);
    }

}
