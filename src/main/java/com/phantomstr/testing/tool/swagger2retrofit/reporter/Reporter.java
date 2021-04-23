package com.phantomstr.testing.tool.swagger2retrofit.reporter;

import lombok.Setter;

import java.util.function.Consumer;

import static java.lang.System.lineSeparator;
import static org.apache.commons.lang3.StringUtils.repeat;

public class Reporter {
    private final StringBuilder out = new StringBuilder();
    @Setter
    private String rowFormat;

    public Reporter(String name) {
        out.append(lineSeparator())
                .append(repeat('=', name.length())).append(lineSeparator())
                .append(name).append(lineSeparator())
                .append(repeat('=', name.length())).append(lineSeparator());
    }

    public void append(Object... args) {
        out.append(String.format(rowFormat, args)).append(lineSeparator());
    }

    public void print(Consumer<String> consumer) {
        consumer.accept(out.toString());
    }

    public void appendRow(String s) {
        out.append(s).append(lineSeparator());
    }

}
