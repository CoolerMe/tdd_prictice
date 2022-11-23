package com.cool.di;

import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class ComponentProvider<T> implements ContextConfig.ConstructionProvider<T> {

    private final Constructor<T> constructor;

    private final List<Field> fields;

    private final List<Method> methods;

    public ComponentProvider(Class<T> implementation) {
        this.constructor = getConstructor(implementation);
        this.fields = getFields(implementation);
        this.methods = getMethods(implementation);
    }

    @Override
    public T get(Context context) {
        try {
            Object[] dependencies = stream(constructor.getParameters())
                    .map(parameter -> context.get(parameter.getType()).get())
                    .toArray(Object[]::new);
            T instance = constructor.newInstance(dependencies);
            for (Field field : fields) {
                field.set(instance, context.get(field.getType()).get());
            }
            for (Method method : methods) {
                method.invoke(instance, stream(method.getParameterTypes())
                        .map(type -> context.get(type).get())
                        .toArray(Object[]::new));
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return concat(methods.stream().flatMap(method -> stream(method.getParameterTypes())),
                concat(fields.stream().map(Field::getType),
                        stream(constructor.getParameters()).map(Parameter::getType)))

                .collect(Collectors.toList());

    }


    private static <Type> Constructor<Type> getConstructor(Class<Type> implementation) {
        List<Constructor<?>> constructors = stream(implementation.getConstructors())
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

    private static <T> List<Method> getMethods(Class<T> implementation) {
        List<Method> methods = new ArrayList<>();
        Class<?> current = implementation;
        while (current != Object.class) {
            methods.addAll(stream(current.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(Inject.class))
                    .filter(method -> methods.stream().noneMatch(o -> o.getName().equals(method.getName()) &&
                            Arrays.equals(method.getParameterTypes(), o.getParameterTypes())))
                    .filter(method -> stream(implementation.getDeclaredMethods()).filter(m -> !m.isAnnotationPresent(Inject.class))
                            .noneMatch(o -> o.getName().equals(method.getName()) && Arrays.equals(method.getParameterTypes(), o.getParameterTypes())))
                    .toList());
            current = current.getSuperclass();
        }
        Collections.reverse(methods);

        return methods;
    }

    private static List<Field> getFields(Class<?> implementation) {
        List<Field> fieldList = new ArrayList<>();
        Class<?> current = implementation;
        while (current != Object.class) {
            fieldList.addAll(stream(current.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(Inject.class)).toList());
            current = current.getSuperclass();
        }
        return fieldList;
    }


}
