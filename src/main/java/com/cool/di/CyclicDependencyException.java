package com.cool.di;

import java.util.HashSet;
import java.util.Set;

public class CyclicDependencyException extends RuntimeException {

    private final Set<Class<?>> components = new HashSet<>();

    public CyclicDependencyException(Class<?> component) {
        this.components.add(component);
    }

    public CyclicDependencyException(CyclicDependencyException exception, Class<?> component) {
        components.addAll(exception.getComponents());
        components.add(component);
    }

    public Set<Class<?>> getComponents() {
        return components;
    }
}
