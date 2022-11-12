package com.cool.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Context {

    private final Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        providers.put(type, () -> instance);
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<Implementation> constructor = getConstructor(implementation);
        providers.put(type, new ComponentProvider<>(constructor));
    }

    public <T> Optional<T> get(Class<T> type) {
        return Optional.ofNullable(providers.get(type))
                .map(provider -> (T) provider.get());
    }


    class ComponentProvider<Type> implements Provider<Type> {

        private final Constructor<Type> constructor;

        private boolean constructing;

        public ComponentProvider(Constructor<Type> constructor) {
            this.constructor = constructor;
        }

        @Override
        public Type get() {
            if (constructing) {
                throw new CyclicDependencyException();
            }
            try {
                constructing = true;
                Object[] dependencies = Arrays.stream(constructor.getParameters())
                        .map(parameter -> Context.this.get(parameter.getType())
                                .orElseThrow(DependencyNotFoundException::new))
                        .toArray(Object[]::new);
                return constructor.newInstance(dependencies);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                constructing = false;
            }
        }

    }

    private static <Type> Constructor<Type> getConstructor(Class<Type> implementation) {
        List<Constructor<?>> constructors = Arrays.stream(implementation.getConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .toList();

        if (constructors.size() > 1) {
            throw new IllegalComponentException();
        }

        return (Constructor<Type>) constructors.stream()
                .findFirst()
                .orElseGet(() -> {
                    try {
                        return implementation.getConstructor();
                    } catch (NoSuchMethodException e) {
                        throw new IllegalComponentException();
                    }
                });
    }
}
