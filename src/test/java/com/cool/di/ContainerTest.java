package com.cool.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

            Component component = context.get(Component.class);

            assertSame(instance, component);
        }

        // TODO abstract class
        // TODO interface
        @Nested
        public class ConstructionInjection {

            //  No args constructor
            @Test
            public void should_bind_type_to_class_with_no_args_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = context.get(Component.class);

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

                Component component = context.get(Component.class);

                assertNotNull(component);
                assertSame(dependency, ((ComponentWithInjectConstructor) component).getDependency());

            }

            // TODO A->B->C
            @Test
            public void should_bind_type_to_a_class_with_transitive_dependencies() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class,DependencyWithInjectConstructor.class);
                String anotherDependency = "Inject dependency";
                context.bind(String.class, anotherDependency);

                Component component = context.get(Component.class);
                assertNotNull(component);

                Dependency dependency = ((ComponentWithInjectConstructor) component).getDependency();
                assertNotNull(dependency);

                assertSame(anotherDependency, ((DependencyWithInjectConstructor) dependency).getAnotherDependency());
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