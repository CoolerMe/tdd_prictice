package com.cool.di;

import java.util.*;

import static java.util.List.*;

public class ContextConfig {

    private final Map<Class<?>, ConstructionProvider<?>> providers = new HashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        providers.put(type, new ConstructionProvider<T>() {
            @Override
            public T get(Context context) {
                return instance;
            }

            @Override
            public List<Class<?>> getDependencies() {
                return of();
            }
        });

    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new ComponentProvider<>(implementation));
    }


    public Context getContext() {
        providers.keySet()
                .forEach(component -> checkDependencies(component, new Stack<>()));

        return new Context() {
            @Override
            public <T> Optional<T> get(Class<T> type) {
                return Optional.ofNullable(providers.get(type))
                        .map(provider -> (T) provider.get(this));
            }

        };
    }


    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Class<?> dependency : providers.get(component).getDependencies()) {
            if (!providers.containsKey(dependency)) {
                throw new DependencyNotFoundException(component, dependency);
            }
            if (visiting.contains(dependency)) {
                throw new CyclicDependencyException(visiting);
            }
            visiting.push(dependency);
            checkDependencies(dependency, visiting);
            visiting.pop();
        }

    }

    interface ConstructionProvider<Type> {

        Type get(Context context);


        List<Class<?>> getDependencies();

    }

}
