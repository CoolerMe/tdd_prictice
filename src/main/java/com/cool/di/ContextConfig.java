package com.cool.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

public class ContextConfig {

    private final Map<Class<?>, ConstructionProvider<?>> constructionProviders = new HashMap<>();

    private final Map<Class<?>, List<Class<?>>> dependencies = new HashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        constructionProviders.put(type, context -> instance);
        dependencies.put(type, List.of());
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<Implementation> constructor = getConstructor(implementation);
        constructionProviders.put(type, new ComponentProvider<>(constructor, type));
        dependencies.put(type, Arrays.stream(constructor.getParameters()).map(Parameter::getType).collect(Collectors.toList()));
    }


    public Context getContext() {
        Set<Class<?>> components = dependencies.keySet();
        for (Class<?> component : components) {
            List<Class<?>> list = this.dependencies.get(component);
            for (Class<?> dependency : list) {
                if (!dependencies.containsKey(dependency)) {
                    throw new DependencyNotFoundException(component, dependency);
                }
            }
        }

        return new Context() {
            @Override
            public <T> Optional<T> get(Class<T> type) {
                return Optional.ofNullable(constructionProviders.get(type))
                        .map(provider -> (T) provider.get(this));
            }
        };
    }

    interface ConstructionProvider<Type> {

        Type get(Context context);

    }

    static class ComponentProvider<Type> implements ConstructionProvider<Type> {

        private final Constructor<Type> constructor;

        private final Class<?> component;

        private boolean constructing;

        public ComponentProvider(Constructor<Type> constructor, Class<?> component) {
            this.constructor = constructor;
            this.component = component;
        }

        @Override
        public Type get(Context context) {
            if (constructing) {
                throw new CyclicDependencyException(component);
            }
            try {
                constructing = true;
                Object[] dependencies = Arrays.stream(constructor.getParameters())
                        .map(parameter -> context.get(parameter.getType())
                                .orElseThrow(() -> new DependencyNotFoundException(component, parameter.getType())))
                        .toArray(Object[]::new);
                return constructor.newInstance(dependencies);
            } catch (CyclicDependencyException e) {
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
