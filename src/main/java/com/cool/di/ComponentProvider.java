package com.cool.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ComponentProvider<Type> implements ContextConfig.ConstructionProvider<Type> {

    private final Constructor<Type> constructor;

    private final List<Field> fields;

    public ComponentProvider(Class<Type> implementation) {
        this.constructor = getConstructor(implementation);
        this.fields = getFields(implementation);
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
                        return implementation.getDeclaredConstructor();
                    } catch (NoSuchMethodException e) {
                        throw new IllegalComponentException();
                    }
                });
    }

    private static List<Field> getFields(Class<?> implementation) {
        List<Field> fieldList = new ArrayList<>();
        Class<?> current = implementation;
        while (current != Object.class) {
            fieldList.addAll(Arrays.stream(current.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(Inject.class)).toList());
            current = current.getSuperclass();
        }
        return fieldList;
    }

    @Override
    public Type get(Context context) {

        try {
            Object[] dependencies = Arrays.stream(constructor.getParameters())
                    .map(parameter -> context.get(parameter.getType()).get())
                    .toArray(Object[]::new);
            Type type = constructor.newInstance(dependencies);
            for (Field field : fields) {
                field.set(type, context.get(field.getType()).get());
            }
            return type;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Stream.concat(fields.stream().map(Field::getType),
                Arrays.stream(constructor.getParameters())
                        .map(Parameter::getType)).collect(Collectors.toList());

    }

}
