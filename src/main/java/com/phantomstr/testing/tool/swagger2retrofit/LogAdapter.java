package com.phantomstr.testing.tool.swagger2retrofit;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.logging.Log;

@Slf4j
public class LogAdapter implements Log {

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public void debug(CharSequence charSequence) {
        log.debug("{}", charSequence);
    }

    @Override
    public void debug(CharSequence charSequence, Throwable throwable) {
        log.debug(String.valueOf(charSequence), throwable);
    }

    @Override
    public void debug(Throwable throwable) {
        log.debug("", throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    @Override
    public void info(CharSequence charSequence) {
        log.info(String.valueOf(charSequence));
    }

    @Override
    public void info(CharSequence charSequence, Throwable throwable) {
        log.info(String.valueOf(charSequence), throwable);
    }

    @Override
    public void info(Throwable throwable) {
        log.info("", throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    @Override
    public void warn(CharSequence charSequence) {
        log.warn(String.valueOf(charSequence));
    }

    @Override
    public void warn(CharSequence charSequence, Throwable throwable) {
        log.info(String.valueOf(charSequence), throwable);
    }

    @Override
    public void warn(Throwable throwable) {
        log.info("", throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    @Override
    public void error(CharSequence charSequence) {
        log.error(String.valueOf(charSequence));
    }

    @Override
    public void error(CharSequence charSequence, Throwable throwable) {
        log.error(String.valueOf(charSequence), throwable);
    }

    @Override
    public void error(Throwable throwable) {
        log.error("", throwable);
    }

}
