package com.cool.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Context {

    private final Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        providers.put(type, () -> instance);
    }

    public <T> T get(Class<T> type) {
        return (T) providers.get(type).get();
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, () -> {
            try {
                Constructor<?> constructor = getConstructor(implementation);
                Object[] dependencies = Arrays.stream(constructor.getParameters())
                        .map(parameter -> get(parameter.getType()))
                        .toArray(Object[]::new);
                return constructor.newInstance(dependencies);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static <Type> Constructor<?> getConstructor(Class<Type> implementation) {
        Stream<Constructor<?>> stream = Arrays.stream(implementation.getConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class));
        return stream.findFirst()
                .orElseGet(() -> getInjectConstructor(implementation));
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        try {
            return implementation.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
