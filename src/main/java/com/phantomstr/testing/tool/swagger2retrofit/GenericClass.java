package com.phantomstr.testing.tool.swagger2retrofit;

public class GenericClass<T> {

    private final Class<T> type;

    public GenericClass(Class<T> type) {
        this.type = type;
    }

    public Class<T> getGenericType() {
        return this.type;
    }

}
