package com.cool.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class ComponentProvider<Type> implements ContextConfig.ConstructionProvider<Type> {

    private final Constructor<Type> constructor;


    public ComponentProvider(Class<Type> implementation) {
        this.constructor = getConstructor(implementation);
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

    @Override
    public Type get(Context context) {

        try {
            Object[] dependencies = Arrays.stream(constructor.getParameters())
                    .map(parameter -> context.get(parameter.getType()).get())
                    .toArray(Object[]::new);
            return constructor.newInstance(dependencies);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Arrays.stream(constructor.getParameters()).map(Parameter::getType).collect(Collectors.toList());
    }

}
