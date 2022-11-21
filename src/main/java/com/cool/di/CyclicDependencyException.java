package com.cool.di;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class CyclicDependencyException extends RuntimeException {

    private final Set<Class<?>> components = new HashSet<>();

    public CyclicDependencyException(Class<?> component) {
        this.components.add(component);
    }

    public CyclicDependencyException(Stack<Class<?>> visiting) {
        components.addAll(visiting);
    }

    public Set<Class<?>> getComponents() {
        return components;
    }
}
