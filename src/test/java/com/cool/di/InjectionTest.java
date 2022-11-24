package com.cool.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Nested
public class InjectionTest {

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


            @Test
            public void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class,
                        () -> new ComponentProvider<>(AbstractComponent.class));
            }

            @Test
            public void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class,
                        () -> new ComponentProvider<>(Component.class));
            }

            //  multi inject constructor
            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    new ComponentProvider<>(ComponentWithMultiInjectConstructors.class);
                });
            }

            //  no default or inject constructor
            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    new ComponentProvider<>(ComponentWithNoInjectNorDefaultConstructor.class);
                });
            }

            @Test
            public void should_include_dependency_from_inject_constructor() {
                ComponentProvider<ComponentWithInjectConstructor> component = new ComponentProvider<>(ComponentWithInjectConstructor.class);

                assertArrayEquals(new Class<?>[]{Dependency.class}, component.getDependencies().toArray(Class<?>[]::new));
            }
        }

        @Nested
        public class FieldInjection {


            @Test
            public void should_inject_dependency_via_field() {
                config.bind(ComponentWithInjectField.class, ComponentWithInjectField.class);
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);

                ComponentWithInjectField component = config.getContext().get(ComponentWithInjectField.class).get();

                assertSame(dependency, component.dependency);

            }


            @Test
            public void should_throw_exception_if_inject_field_is_final() {
                assertThrows(IllegalComponentException.class,
                        () -> new ComponentProvider<>(FinalInjectField.class));
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


            @Test
            public void should_call_inject_method_even_if_no_dependency_declared() {
                config.bind(InjectMethodWithoutDependency.class, InjectMethodWithoutDependency.class);

                InjectMethodWithoutDependency dependency = config.getContext().get(InjectMethodWithoutDependency.class).get();

                assertTrue(dependency.called);
            }


            @Test
            public void should_inject_dependencies_in_inject_methods() {
                config.bind(InjectMethodWithDependency.class, InjectMethodWithDependency.class);
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);

                InjectMethodWithDependency methodWithDependency = config.getContext().get(InjectMethodWithDependency.class).get();

                assertSame(dependency, methodWithDependency.dependency);
            }


            @Test
            public void should_override_inject_methods_of_superclass() {
                config.bind(SubClassWithInjectMethod.class, SubClassWithInjectMethod.class);

                SubClassWithInjectMethod subClassWithInjectMethod = config.getContext().get(SubClassWithInjectMethod.class).get();

                assertEquals(1, subClassWithInjectMethod.superCalled);
                assertEquals(2, subClassWithInjectMethod.subCalled);
            }


            @Test
            public void should_call_once_when_subclass_override_inject_method_with_inject() {
                config.bind(SubClassWithOverrideInjectMethod.class, SubClassWithOverrideInjectMethod.class);

                SubClassWithOverrideInjectMethod component = config.getContext().get(SubClassWithOverrideInjectMethod.class).get();

                assertEquals(1, component.superCalled);
            }


            @Test
            public void should_not_call_method_subclass_override_without_inject() {
                config.bind(SubClassOverrideInjectMethodWithoutInject.class, SubClassOverrideInjectMethodWithoutInject.class);

                SubClassOverrideInjectMethodWithoutInject component = config.getContext().get(SubClassOverrideInjectMethodWithoutInject.class).get();

                assertEquals(0, component.superCalled);
            }

            @Test
            public void should_include_dependencies_in_inject_methods() {
                ComponentProvider<InjectMethodWithDependency> provider = new ComponentProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }


            @Test
            public void should_throw_exception_if_type_parameter_provided() {
                assertThrows(IllegalComponentException.class,
                        () -> new ComponentProvider<>(MethodInjectionWithTypeParameter.class));
            }
        }
    }
}

class ComponentWithInjectField {

    @Inject
    Dependency dependency;
}

abstract class AbstractComponent implements Component {

    @Inject
    public AbstractComponent() {
    }
}

class InjectMethodWithoutDependency {
    boolean called = false;

    @Inject
    public void call() {
        called = true;
    }
}

//  get inject field from super class
class SuperClassWithInjectField extends ComponentWithInjectField {

}


class FinalInjectField {

    @Inject
    final Dependency dependency = null;
}

class SubClassOverrideInjectMethodWithoutInject extends SuperClassWithInjectMethod {

    @Override
    public void install() {
        super.install();
    }
}

class SubClassWithOverrideInjectMethod extends SuperClassWithInjectMethod {

    @Inject
    @Override
    public void install() {
        super.install();
    }
}


class SuperClassWithInjectMethod {
    int superCalled = 0;

    @Inject
    public void install() {
        superCalled += 1;
    }
}

class SubClassWithInjectMethod extends SuperClassWithInjectMethod {
    int subCalled = 0;

    @Inject
    public void anotherInstall() {
        subCalled = superCalled + 1;
    }
}

class InjectMethodWithDependency {

    Dependency dependency;

    @Inject
    public void install(Dependency dependency) {
        this.dependency = dependency;
    }
}


class MethodInjectionWithTypeParameter {

    @Inject
    <T> void install() {

    }
}