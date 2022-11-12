package com.cool.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.ref.PhantomReference;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    private Context context;

    @BeforeEach
    public void setup() {
        context = new Context();
    }


    @Nested
    public class ComponentConstruction {
        // instance
        @Test
        public void should_bind_type_to_a_specified_instance() {
            Component instance = new Component() {
            };
            context.bind(Component.class, instance);

            Component component = context.get(Component.class).get();

            assertSame(instance, component);
        }

        @Test
        public void should_return_null_if_component_not_defined() {
            assertTrue(context.get(Component.class).isEmpty());
        }

        // TODO abstract class
        // TODO interface
        @Nested
        public class ConstructionInjection {

            //  No args constructor
            @Test
            public void should_bind_type_to_class_with_no_args_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = context.get(Component.class).get();

                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);

            }

            //  With args constructor
            @Test
            public void should_bind_type_to_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() {
                };
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, dependency);

                Component component = context.get(Component.class).get();

                assertNotNull(component);
                assertSame(dependency, ((ComponentWithInjectConstructor) component).getDependency());

            }

            //  A->B->C
            @Test
            public void should_bind_type_to_a_class_with_transitive_dependencies() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectConstructor.class);
                String anotherDependency = "Inject dependency";
                context.bind(String.class, anotherDependency);

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
                    context.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });
            }

            //  no default or inject constructor
            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    context.bind(Component.class, ComponentWithNoInjectNorDefaultConstructor.class);
                });
            }

            //  dependency not provided
            @Test
            public void should_throw_exception_if_dependency_not_provided() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                assertThrows(DependencyNotFoundException.class,
                        () -> context.get(Component.class));
            }

            //  cyclic dependency
            @Test
            public void should_throw_exception_if_cyclic_dependency_found() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyDependOnComponent.class);

                assertThrows(CyclicDependencyException.class, () -> context.get(Component.class));
            }

            // TODO transitive cyclic dependency
            @Test
            public void should_throw_exception_if_transitive_cyclic_dependency_found() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyDependOnAnotherDependency.class);
                context.bind(AnotherDependency.class, AnotherDependencyDependOnComponent.class);

                assertThrows(CyclicDependencyException.class, () -> context.get(Component.class));
            }
        }

        @Nested
        public class FieldInjection {

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

    private String anotherDependency;

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