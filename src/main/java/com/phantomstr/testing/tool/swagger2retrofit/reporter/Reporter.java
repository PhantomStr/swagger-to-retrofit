package com.phantomstr.testing.tool.swagger2retrofit.reporter;

import lombok.Setter;
import org.slf4j.Logger;

import java.util.function.Consumer;

import static java.lang.System.lineSeparator;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.commons.lang3.StringUtils.substringAfter;

public class Reporter {
    public static final String WARN = "WARN ";
    public static final String INFO = "INFO ";
    private final StringBuilder out = new StringBuilder();
    @Setter
    private String rowFormat;

    public Reporter(String name) {
        out.append(lineSeparator())
                .append(INFO)
                .append(repeat('=', name.length())).append(lineSeparator())
                .append(INFO)
                .append(name).append(lineSeparator())
                .append(INFO)
                .append(repeat('=', name.length())).append(lineSeparator());
    }

    public void info(Object... args) {
        out.append(String.format(INFO + rowFormat, args)).append(lineSeparator());
    }

    public void print(Consumer<String> consumer) {
        consumer.accept(out.toString());
    }

    public void print(Logger logger) {
        String[] lines = out.toString().split(lineSeparator());
        for (String s : lines) {
            if (s == null) {
                continue;
            }
            if (s.startsWith(WARN)) {
                logger.warn(substringAfter(s, WARN));
            } else {
                logger.info(substringAfter(s, INFO));
            }
        }
    }

    public void warn(String message) {
        appendRow(WARN + message);
    }

    public void appendInfoRow(String message) {
        appendRow(INFO + message);
    }

    private void appendRow(String s) {
        out.append(s).append(lineSeparator());
    }

}
