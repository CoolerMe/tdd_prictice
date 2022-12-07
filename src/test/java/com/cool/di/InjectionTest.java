package com.cool.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Nested
public class InjectionTest {

    private final Dependency dependency = mock(Dependency.class);

    private Context context = mock(Context.class);

    @BeforeEach
    public void setup() {
        when(context.get(eq(Dependency.class)))
                .thenReturn(Optional.ofNullable(dependency));
    }


    @Nested
    public class ConstructionInjection {

        @Nested
        public class Injection {
            //  No args constructor
            @Test
            public void should_call_default_constructor_if_no_inject_constructor() {
                ComponentWithDefaultConstructor instance = new ComponentProvider<>(ComponentWithDefaultConstructor.class).get(context);

                assertNotNull(instance);
            }

            //  With args constructor
            @Test
            public void should_inject_dependency_via_inject_constructor() {
                ComponentWithInjectConstructor component = new ComponentProvider<>(ComponentWithInjectConstructor.class).get(context);

                assertNotNull(component);
                assertSame(dependency, component.getDependency());
            }


            @Test
            public void should_include_dependency_from_inject_constructor() {
                ComponentProvider<ComponentWithInjectConstructor> component = new ComponentProvider<>(ComponentWithInjectConstructor.class);

                assertArrayEquals(new Class<?>[]{Dependency.class}, component.getDependencies().toArray(Class<?>[]::new));
            }
        }

        @Nested
        public class IllegalInjectConstructor {


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
        }

    }

    @Nested
    public class FieldInjection {


        @Nested
        public class Injection {
            @Test
            public void should_inject_field_dependency_via_dependency_field() {
                ComponentWithInjectField component = new ComponentProvider<>(ComponentWithInjectField.class).get(context);

                assertSame(dependency, component.dependency);

            }


            @Test
            public void should_inject_field_dependency_via_superclass_inject_field() {
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
        public class IllegalInjectFields {
            @Test
            public void should_throw_exception_if_inject_field_is_final() {
                assertThrows(IllegalComponentException.class,
                        () -> new ComponentProvider<>(FinalInjectField.class));
            }

        }

    }

    @Nested
    public class MethodInjection {

        @Nested
        public class Injection {


            @Test
            public void should_call_inject_method_even_if_no_dependency_declared() {
                InjectMethodWithoutDependency dependency = new ComponentProvider<>(InjectMethodWithoutDependency.class).get(context);

                assertTrue(dependency.called);
            }


            @Test
            public void should_inject_dependencies_in_inject_methods() {
                InjectMethodWithDependency methodWithDependency = new ComponentProvider<>(InjectMethodWithDependency.class).get(context);

                assertSame(dependency, methodWithDependency.dependency);
            }


            @Test
            public void should_override_inject_methods_of_superclass() {
                SubClassWithInjectMethod subClassWithInjectMethod = new ComponentProvider<>(SubClassWithInjectMethod.class).get(context);

                assertEquals(1, subClassWithInjectMethod.superCalled);
                assertEquals(2, subClassWithInjectMethod.subCalled);
            }


            @Test
            public void should_inject_once_when_subclass_override_inject_method_with_inject() {
                SubClassWithOverrideInjectMethod component = new ComponentProvider<>(SubClassWithOverrideInjectMethod.class).get(context);

                assertEquals(1, component.superCalled);
            }


            @Test
            public void should_not_call_method_subclass_override_without_inject() {
                SubClassOverrideInjectMethodWithoutInject component = new ComponentProvider<>(SubClassOverrideInjectMethodWithoutInject.class).get(context);

                assertEquals(0, component.superCalled);
            }

            @Test
            public void should_include_dependencies_in_inject_methods() {
                ComponentProvider<InjectMethodWithDependency> provider = new ComponentProvider<>(InjectMethodWithDependency.class);

                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }
        }


        @Nested
        public class IllegalInjectMethods {
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