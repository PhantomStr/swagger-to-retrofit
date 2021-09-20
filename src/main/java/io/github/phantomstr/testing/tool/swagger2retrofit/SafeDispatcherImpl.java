package io.github.phantomstr.testing.tool.swagger2retrofit;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class SafeDispatcherImpl implements Dispatcher {

    private final Map<Class<?>, Handler<?>> dispatch = new HashMap<>();

    public <T> void addHandler(GenericClass<T> aClass, Handler<T> handler) {dispatch.put(aClass.getGenericType(), handler);}

    public void handle(Object o) {
        Handler h = dispatch.get(o.getClass());
        if (h == null) {
            return;
        }
        h.handle(o);
    }

}
