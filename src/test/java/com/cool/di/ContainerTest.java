package com.cool.di;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    interface Component {

    }

    static class ComponentWithDefaultConstructor implements Component {

        public ComponentWithDefaultConstructor() {
        }
    }

    @Nested
    public class ComponentConstruction {
        // instance
        @Test
        public void should_bind_type_to_a_specified_instance() {
            Context context = new Context();
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

            // TODO No args constructor
            @Test
            public void should_bind_type_to_class_with_no_args_constructor() {
                Context context = new Context();
                context.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = context.get(Component.class);

                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);

            }
            // TODO With args constructor
            // TODO A->B->C

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