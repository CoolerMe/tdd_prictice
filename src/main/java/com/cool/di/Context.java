package com.cool.di;

import jakarta.inject.Provider;

import java.util.HashMap;
import java.util.Map;

public class Context {

    private final Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        providers.put(type, () -> instance);
    }

    public <T> T get(Class<T> type) {
        return (T) providers.get(type).get();
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, () -> {
            try {
                return (implementation).getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
