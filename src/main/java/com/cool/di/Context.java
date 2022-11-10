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
        Constructor<?>[] constructors = Arrays.stream(implementation.getConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .toArray(Constructor<?>[]::new);
        if (constructors.length > 1) {
            throw new IllegalComponentException();
        }

        if (constructors.length == 0 && Arrays.stream(implementation.getConstructors())
                .noneMatch(constructor -> constructor.getParameters().length == 0)) {
            throw new IllegalComponentException();
        }
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
                .orElseGet(() -> {
                    try {
                        return implementation.getConstructor();
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

}
