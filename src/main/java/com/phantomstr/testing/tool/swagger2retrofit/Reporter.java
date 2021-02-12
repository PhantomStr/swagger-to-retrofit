package com.phantomstr.testing.tool.swagger2retrofit;

import java.util.function.Consumer;

import static java.lang.System.lineSeparator;
import static org.apache.commons.lang3.StringUtils.repeat;

class Reporter {
    private StringBuilder out = new StringBuilder();
    private String format;

    Reporter(String name) {
        out.append(lineSeparator())
                .append(repeat('=', name.length())).append(lineSeparator())
                .append(name).append(lineSeparator())
                .append(repeat('=', name.length())).append(lineSeparator());
    }

    void setRowFormat(String format) {
        this.format = format;
    }

    void append(Object... args) {
        out.append(String.format(format, args)).append(lineSeparator());
    }

    void print(Consumer<String> consumer) {
        consumer.accept(out.toString());
    }

    void appendRow(String s) {
        out.append(s).append(lineSeparator());
    }

}
