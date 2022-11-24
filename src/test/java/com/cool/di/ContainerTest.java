package com.cool.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    private ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }

    @Nested
    public class DependencyCheck {

        @Test
        public void should_throw_exception_if_dependency_not_provided() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                    () -> config.getContext());

            assertSame(Component.class, exception.getComponent());
            assertSame(Dependency.class, exception.getDependency());
        }

        @Test
        public void should_throw_exception_if_cyclic_dependency_found() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyDependOnComponent.class);

            CyclicDependencyException exception = assertThrows(CyclicDependencyException.class, () -> config.getContext());

            Set<Class<?>> components = exception.getComponents();
            assertEquals(2, components.size());
            assertTrue(components.contains(Component.class));
            assertTrue(components.contains(Dependency.class));
        }

        // transitive cyclic dependency
        @Test
        public void should_throw_exception_if_transitive_cyclic_dependency_found() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyDependOnAnotherDependency.class);
            config.bind(AnotherDependency.class, AnotherDependencyDependOnComponent.class);

            CyclicDependencyException exception = assertThrows(CyclicDependencyException.class, () -> config.getContext());

            Set<Class<?>> components = exception.getComponents();
            assertEquals(3, components.size());
            assertTrue(components.contains(Component.class));
            assertTrue(components.contains(Dependency.class));
            assertTrue(components.contains(AnotherDependency.class));
        }
    }

    @Nested
    public class DependencySelection {

    }

    @Nested
    public class LifecycleManagement {

    }
}

interface Component {

}

interface Dependency {

}

interface AnotherDependency {

}

class ComponentWithDefaultConstructor implements Component {

    public ComponentWithDefaultConstructor() {
    }
}

class ComponentWithInjectConstructor implements Component {

    private Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}

class DependencyDependOnComponent implements Dependency {

    private Component component;

    @Inject
    public DependencyDependOnComponent(Component component) {
        this.component = component;
    }
}

class ComponentWithMultiInjectConstructors implements Component {

    private String name;
    private Double value;

    @Inject
    public ComponentWithMultiInjectConstructors(String name, Double value) {
        this.name = name;
        this.value = value;
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name) {
        this.name = name;
    }
}

class ComponentWithNoInjectNorDefaultConstructor implements Component {

    private String name;

    public ComponentWithNoInjectNorDefaultConstructor(String name) {
        this.name = name;
    }
}

class DependencyWithInjectConstructor implements Dependency {

    private final String anotherDependency;

    @Inject
    public DependencyWithInjectConstructor(String anotherDependency) {
        this.anotherDependency = anotherDependency;
    }

    public String getAnotherDependency() {
        return anotherDependency;
    }
}

class DependencyDependOnAnotherDependency implements Dependency {

    private AnotherDependency anotherDependency;

    @Inject
    public DependencyDependOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }
}

class AnotherDependencyDependOnComponent implements AnotherDependency {

    private Component component;

    @Inject
    public AnotherDependencyDependOnComponent(Component component) {
        this.component = component;
    }
}