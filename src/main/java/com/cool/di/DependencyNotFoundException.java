package com.cool.di;

public class DependencyNotFoundException extends RuntimeException {

    private final Class<?> dependency;

    private final Class<?> component;

    public DependencyNotFoundException(Class<?> component, Class<?> dependency) {
        this.component = component;
        this.dependency = dependency;
    }

    public Class<?> getDependency() {
        return this.dependency;
    }

    public Class<?> getComponent() {
        return this.component;
    }
}
