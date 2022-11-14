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
        providers.put(type, new ComponentProvider<>(constructor, type));
    }

    public <T> Optional<T> get(Class<T> type) {
        return Optional.ofNullable(providers.get(type))
                .map(provider -> (T) provider.get());
    }


    class ComponentProvider<Type> implements Provider<Type> {

        private final Constructor<Type> constructor;

        private final Class<?> component;

        private boolean constructing;

        public ComponentProvider(Constructor<Type> constructor, Class<?> component) {
            this.constructor = constructor;
            this.component = component;
        }

        @Override
        public Type get() {
            if (constructing) {
                throw new CyclicDependencyException(component);
            }
            try {
                constructing = true;
                Object[] dependencies = Arrays.stream(constructor.getParameters())
                        .map(parameter -> Context.this.get(parameter.getType())
                                .orElseThrow(() -> new DependencyNotFoundException(component, parameter.getType())))
                        .toArray(Object[]::new);
                return constructor.newInstance(dependencies);
            }catch (CyclicDependencyException e){
                throw new CyclicDependencyException(e, component);
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
