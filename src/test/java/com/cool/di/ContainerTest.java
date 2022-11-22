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
    public class ComponentConstruction {
        // instance
        @Test
        public void should_bind_type_to_a_specified_instance() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            Context context = config.getContext();
            Component component = context.get(Component.class).get();

            assertSame(instance, component);
        }

        @Test
        public void should_return_null_if_component_not_defined() {
            assertTrue(config.getContext().get(Component.class).isEmpty());
        }

        // TODO abstract class
        // TODO interface
        @Nested
        public class ConstructionInjection {

            //  No args constructor
            @Test
            public void should_bind_type_to_class_with_no_args_constructor() {
                config.bind(Component.class, ComponentWithDefaultConstructor.class);

                Context context = config.getContext();
                Component instance = context.get(Component.class).get();

                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);

            }

            //  With args constructor
            @Test
            public void should_bind_type_to_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, dependency);

                Context context = config.getContext();
                Component component = context.get(Component.class).get();

                assertNotNull(component);
                assertSame(dependency, ((ComponentWithInjectConstructor) component).getDependency());

            }

            //  A->B->C
            @Test
            public void should_bind_type_to_a_class_with_transitive_dependencies() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class);
                String anotherDependency = "Inject dependency";
                config.bind(String.class, anotherDependency);

                Context context = config.getContext();
                Component component = context.get(Component.class).get();
                assertNotNull(component);

                Dependency dependency = ((ComponentWithInjectConstructor) component).getDependency();
                assertNotNull(dependency);

                assertSame(anotherDependency, ((DependencyWithInjectConstructor) dependency).getAnotherDependency());
            }

            //  multi inject constructor
            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });
            }

            //  no default or inject constructor
            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithNoInjectNorDefaultConstructor.class);
                });
            }

            //  dependency not provided
            @Test
            public void should_throw_exception_if_dependency_not_provided() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                        () -> config.getContext());

                assertSame(Component.class, exception.getComponent());
                assertSame(Dependency.class, exception.getDependency());
            }

            @Test
            public void should_throw_exception_if_transitive_dependency_not_provided() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                        () -> config.getContext());

                assertEquals(String.class, exception.getDependency());
                assertEquals(Dependency.class, exception.getComponent());
            }

            //  cyclic dependency
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
        public class FieldInjection {

            static class ComponentWithInjectField {

                @Inject
                Dependency dependency;
            }

            @Test
            public void should_inject_dependency_via_field() {
                config.bind(ComponentWithInjectField.class, ComponentWithInjectField.class);
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);

                ComponentWithInjectField component = config.getContext().get(ComponentWithInjectField.class).get();

                assertSame(dependency, component.dependency);

            }

            // TODO throw exception if field is final

            //  get inject field from super class
            static class SuperClassWithInjectField extends ComponentWithInjectField {

            }

            @Test
            public void should_inject_field_with_via_superclass_inject_field() {
                ComponentProvider<SuperClassWithInjectField> provider = new ComponentProvider<>(SuperClassWithInjectField.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }

            //  get dependency in filed injection
            @Test
            public void should_include_field_dependency_in_dependencies() {
                ComponentProvider<ComponentWithInjectField> provider = new ComponentProvider<>(ComponentWithInjectField.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }


        }

        @Nested
        public class MethodInjection {

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