package io.github.phantomstr.testing.tool.swagger2retrofit;

public interface Dispatcher {

    <T> void addHandler(GenericClass<T> aClass, Handler<T> handler);

    void handle(Object o);

}
