/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2014, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.lhkbob.entreri.impl.apt;

import com.lhkbob.entreri.DoNotAutoVersion;
import com.lhkbob.entreri.Named;
import com.lhkbob.entreri.Validate;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.regex.Pattern;

/**
 * CollectionsMethodPattern
 * ========================
 *
 * This method pattern matches methods that resemble the element query and manipulation methods provided as
 * part of Java's collections framework for sets, lists, and maps. Primitive collections are not supported by
 * this pattern.
 *
 * ## Set and List patterns
 *
 * It can match methods with three different behaviors: adding, removing, and querying the collection for
 * elements.
 *
 * ### Adding
 *
 * Methods that fit the following pattern will add an element to the container.
 *
 * * The method name must start with `add` or `append`, and the property name is the lower-cased remainder
 * of the method name after the prefix is removed, and then pluralized.
 * * The method has one parameter, whose type is the element type of the container.
 * * The method may return `boolean`, `void`, or the Component type the method is defined in.
 *
 * If the method, or its single parameter, are annotated with `@Named` that is used to define the property
 * name independently of the method name, although the method name must still start with `add` or `append`.
 * The backing container type is not necessarily determined by this method since many container classes can
 * have elements added to them. There are three ways for the backing container type to be determined:
 *
 * 1. Some other method declaring the same property explicitly specifies the container type (e.g. a bean
 * setter for a `List<Foo>`.
 * 2. The method is annotated with the {@link com.lhkbob.entreri.property.Collection} attribute and the type
 * is set to something other than UNSPECIFIED.
 * 3. Some other method for the same property as the Collection attribute declaring the backing container
 * type.
 *
 * In the event that multiple methods for the same property can determine the backing container type, they
 * of course must be consistent with one another.
 *
 * If the method's return type is `boolean`, the returned value is true if the backing container was
 * modified by the add and false otherwise. If the method's return type is the Component type, the component
 * instance is returned for method chaining purposes. If the method's return type is void nothing is returned.
 *
 * This method pattern requires that the selected Property implementation expose an `add(int, E) -> boolean`
 * method where `E` is the element type of the container.
 *
 * ### Removing
 *
 * Methods that fit the following pattern will remove an element from the container. The pattern is
 * identical to that of adding methods except the method name prefix must be `remove`, `delete`, or `discard`.
 * The return type, backing container type identification, and naming behavior is equivalent to the adding
 * pattern. This method pattern requires that the selected Property implementation expose a `remove(int, E) ->
 * boolean` method where `E` is the element type of the container.
 *
 * ### Querying
 *
 * Methods that fit the following pattern will return a boolean for if the element is contained in the
 * backing container.
 *
 * * The method name must start with `contains`, `includes`, or `in`, and the property name defaults to the
 * lower-cased remainder of the method name after the prefix is removed, and then pluralized.
 * * The method has one parameter, whose type is the element type of the container.
 * * The method must return a `boolean`. True means the argument is in the container, and false means it is
 * not contained.
 *
 * Like the other methods, the property name may be overridden by using the `@Named` annotation. The backing
 * container type must be determined in an equivalent manner. The method pattern erquires that the selected
 * Property implementation expose a `contains(int, E) -> boolean` method where `E` is the element type of the
 * container.
 *
 * ## Map patterns
 *
 * Map collection types support four different pattern types: putting, getting, removing, and querying keys.
 *
 * ### Putting
 *
 * Methods that fit the following pattern will put a key-value pair into the map container.
 *
 * * The method name starts with `put`, and the property name is the lower-cased remainder of the method
 * name after the prefix is removed, and then pluralized.
 * * The method has two parameters: the first's type is the key type, the second's type is the value type.
 * * The return type may be `void`, `boolean`, or the same type as the second parameter.
 *
 * This method completely determines the property type as Map of the specified key and value types unless it
 * returns `void` or `boolean`. If the method's return type is `boolean`, the returned value is true if the
 * backing container replaced a non-null element with the new value. If the method's return type is otherwise
 * not `void`, it returns the element that was replaced by the new value. If the method's return type is void,
 * nothing is returned.
 *
 * This method pattern requires that the selected Property implementation expose a `put(int, K, V) -> V`
 * method where `K` is the key type and `V` is the value type.
 *
 * ### Getting
 *
 * Methods that fit the following pattern will retrieve a value based on a key from the map container.
 *
 * * The method name starts with `get`, and the property name defaults to the lower-cased remainder
 * of the name after the prefix is removed, and then pluralized.
 * * The method has one parameter, whose type is the key type of the map.
 * * The method parameter *must not* be annotated with {@link com.lhkbob.entreri.ReturnValue}.
 * * The return value must be an object type that is the value type of the map.
 *
 * This method completely determines the property type as Map of the specified key and value types.
 * This method pattern requires that the selected Property implementation expose a `get(int, K) -> V`
 * method where `K` is the key type and `V` is the value type.
 *
 * ### Removing and querying
 *
 * These patterns are identical to the removal and querying patterns for lists and sets with two exceptions:
 *
 * 1. The argument types represent the key types of the map and the value type is undefined unless the
 * removal method references it in its return value.
 * 2. The return type of the removal method can be `void`, `boolean`, or the value type. It is not possible
 * to return the component instance. The `void` return type returns nothing, the `boolean` return type returns
 * true if a non-null value was removed from the map, and otherwise it returns the value that was removed.
 *
 * These patterns require the property to expose a `remove(int, K) -> V` and `contains(int, K) -> boolean`
 * method respectively.
 *
 * @author Michael Ludwig
 */
public class CollectionsMethodPattern extends AbstractMethodPattern {
    private static final Pattern ADD_PATTERN = Pattern.compile("(add|append).+");
    private static final Pattern REMOVE_PATTERN = Pattern.compile("(remove|delete|discard).+");
    private static final Pattern CONTAINS_PATTERN = Pattern.compile("(contains|includes|in).+");
    private static final Pattern PUT_PATTERN = Pattern.compile("(put).+");
    private static final Pattern GET_PATTERN = Pattern.compile("(get).+");

    public CollectionsMethodPattern() {
        super(Arrays.asList(DoNotAutoVersion.class, Validate.class));
    }

    @Override
    public Map<ExecutableElement, Collection<? extends PropertyDeclaration>> match(Context context,
                                                                                   List<ExecutableElement> methods) {
        TypeMirror booleanType = context.getTypes().getPrimitiveType(TypeKind.BOOLEAN);
        Map<ExecutableElement, Collection<? extends PropertyDeclaration>> matches = new HashMap<>();

        // add methods: returns boolean, void, or component type, takes a single argument
        List<ExecutableElement> addMethods = context.getMatchingMethods(context.getComponentType(), methods,
                                                                        ADD_PATTERN, null, (TypeMirror) null);
        processMethods(context, matches, addMethods, "add", true, "add", "append");

        // remove methods: returns boolean, void, or component type, takes a single argument
        List<ExecutableElement> removeMethods = context.getMatchingMethods(context.getComponentType(),
                                                                           methods, REMOVE_PATTERN, null,
                                                                           (TypeMirror) null);
        processMethods(context, matches, removeMethods, "remove", true, "remove", "delete", "discard");

        // put methods: returns boolean, void, or value type, takes two arguments
        List<ExecutableElement> putMethods = context.getMatchingMethods(context.getComponentType(), methods,
                                                                        PUT_PATTERN, null, null, null);
        processMethods(context, matches, putMethods, "put", true, "put");

        // contains methods: returns a boolean, takes a single argument
        List<ExecutableElement> containsMethods = context.getMatchingMethods(context.getComponentType(),
                                                                             methods, CONTAINS_PATTERN,
                                                                             booleanType, (TypeMirror) null);
        processMethods(context, matches, containsMethods, "contains", false, "contains", "includes", "in");

        // get methods: returns an object type, takes a single argument
        List<ExecutableElement> getMethods = context.getMatchingMethods(context.getComponentType(), methods,
                                                                        GET_PATTERN, null, (TypeMirror) null);
        processMethods(context, matches, getMethods, "get", false, "get");

        return matches;
    }

    private void processMethods(Context context,
                                Map<ExecutableElement, Collection<? extends PropertyDeclaration>> matches,
                                List<ExecutableElement> methods, String propertyMethod, boolean mutator,
                                String... propertyPrefixes) {
        method:
        for (ExecutableElement a : methods) {
            // all parameters must be declared types (not primitives)
            for (VariableElement p : a.getParameters()) {
                if (p.asType().getKind() != TypeKind.DECLARED) {
                    continue method;
                }
            }

            switch (propertyMethod) {
            case "remove":
                // this must return void, a boolean, the component type, or an object type (only allowed for maps)
                // but the map validation must be done later
                if (a.getReturnType().getKind() != TypeKind.VOID &&
                    a.getReturnType().getKind() != TypeKind.BOOLEAN &&
                    a.getReturnType().getKind() != TypeKind.DECLARED) {
                    continue;
                }
                break;
            case "put":
                // this must return void, a boolean, or an object type that equals the 2nd argument type
                if (a.getReturnType().getKind() != TypeKind.VOID &&
                    a.getReturnType().getKind() != TypeKind.BOOLEAN &&
                    !context.getTypes().isSameType(a.getReturnType(), a.getParameters().get(1).asType())) {
                    continue;
                }
                break;
            case "get":
                // this must return an object type that is the value type of the map
                if (a.getReturnType().getKind() != TypeKind.DECLARED) {
                    continue;
                }
                break;
            case "add":
                // this must return void, a boolean, or the component type
                if (a.getReturnType().getKind() != TypeKind.VOID &&
                    a.getReturnType().getKind() != TypeKind.BOOLEAN &&
                    !context.getTypes().isSameType(context.getComponentType(), a.getReturnType())) {
                    continue;
                }
                break;
            case "contains":
                // this must return a boolean
                if (a.getReturnType().getKind() != TypeKind.BOOLEAN) {
                    continue;
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown property method, " + propertyMethod +
                                                        " this is a BUG!");
            }

            String name = getPropertyName(a, 0, false);
            boolean usedNameAnnotation = name != null; // name is only not-null when @Named was present
            if (name == null && propertyMethod.equals("put")) {
                // check second parameter
                name = getPropertyName(a, 1, false);
                usedNameAnnotation = name != null;
            }
            if (name == null) {
                name = getPropertyName(a, propertyPrefixes);
                usedNameAnnotation = a.getAnnotation(Named.class) != null;
            }
            // make the name plural (with very simple logic) if it did not come from an @Named attribute
            if (!usedNameAnnotation) {
                if (name.endsWith("s") || name.endsWith("ch") || name.endsWith("z") || name.endsWith("sh")) {
                    // sibilant sounds have 'es' added
                    name = name + "es";
                } else {
                    // sibilant sounds that end with an 'e', voiceless consonants, and other words often just add an 's'
                    name = name + "s";
                }
            }

            PropertyDeclaration property = new PropertyDeclaration(context, name,
                                                                   getCollectionType(context, a,
                                                                                     propertyMethod));
            property.getAttributes().addAll(getPropertyAttributes(context, a));
            for (VariableElement p : a.getParameters()) {
                property.getAttributes().addAll(getPropertyAttributes(context, p));
            }

            Set<Annotation> methodAttrs = new HashSet<>();
            methodAttrs.addAll(getMethodAttributes(a));
            for (VariableElement p : a.getParameters()) {
                methodAttrs.addAll(getMethodAttributes(p));
            }
            MethodDeclaration method = new CollectionDeclaration(a, property, methodAttrs, propertyMethod,
                                                                 mutator);
            property.getMethods().add(method);

            matches.put(a, Collections.singleton(property));
        }
    }

    private TypeMirror getCollectionType(Context context, ExecutableElement m, String propertyMethod) {
        TypeMirror keyType = m.getParameters().get(0).asType();
        TypeMirror valueType = (m.getParameters().size() > 1 ? m.getParameters().get(1).asType()
                                                             : m.getReturnType());

        com.lhkbob.entreri.property.Collection attr = getCollectionAnnotation(m);
        if (attr == null) {
            switch (propertyMethod) {
            case "get":
                return context.fromGenericClass(Map.class, keyType, valueType);
            case "put":
                if (valueType.getKind() == TypeKind.DECLARED) {
                    return context.fromGenericClass(Map.class, keyType, valueType);
                }
                // fall through
            default:
                // the type is not specified
                return null;
            }
        } else {
            switch (attr.type()) {
            case LIST:
                // regardless of the method, the property type is always list<parameter>
                return context.fromGenericClass(List.class, keyType);
            case SET:
                // regardless of the method, the property type is always set<parameter>
                return context.fromGenericClass(Set.class, keyType);
            case MAP:
                // for any method except contains, the property type is map<param, returnType>
                // for contains, it is ambiguous because the return type is always a boolean
                if (propertyMethod.equals("contains") || valueType.getKind() != TypeKind.DECLARED) {
                    return null;
                } else {
                    return context.fromGenericClass(Map.class, keyType, valueType);
                }
            case UNSPECIFIED:
            default:
                return null;
            }
        }
    }

    private com.lhkbob.entreri.property.Collection getCollectionAnnotation(ExecutableElement e) {
        com.lhkbob.entreri.property.Collection attr = e.getAnnotation(com.lhkbob.entreri.property.Collection.class);
        if (attr == null) {
            for (VariableElement p : e.getParameters()) {
                attr = p.getAnnotation(com.lhkbob.entreri.property.Collection.class);
                if (attr != null) {
                    break;
                }
            }
        }
        return attr;
    }

    private static class CollectionDeclaration extends AbstractMethodDeclaration {
        private final String propertyMethod;
        private final boolean mutator;

        private PropertyDeclaration property;

        protected CollectionDeclaration(ExecutableElement method, PropertyDeclaration property,
                                        Set<Annotation> attrs, String propertyMethod, boolean mutator) {
            super(method, attrs);
            this.propertyMethod = propertyMethod;
            this.mutator = mutator;
            this.property = property;
        }

        private TypeMirror getKeyType() {
            return getMethod().getParameters().get(0).asType();
        }

        private TypeMirror getValueType() {
            return ((DeclaredType) property.getType()).getTypeArguments().get(1);
        }

        private boolean isMapProperty() {
            return ((DeclaredType) property.getType()).getTypeArguments().size() == 2;
        }

        @Override
        public void replace(PropertyDeclaration original, PropertyDeclaration replaceWith) {
            if (property == original) {
                property = replaceWith;
            }
        }

        @Override
        public boolean arePropertiesValid(Context context) {
            boolean isMap = isMapProperty();
            // verify that the chosen logical property type is consistent with return type options
            if (propertyMethod.equals("remove")) {
                // all other methods had their types completely constrained or in terms of the type variables
                // and will thus be implicitly validated below
                TypeMirror expectedValueType = (isMap ? getValueType() : context.getComponentType());

                if (getMethod().getReturnType().getKind() != TypeKind.VOID &&
                    getMethod().getReturnType().getKind() != TypeKind.BOOLEAN &&
                    !context.getTypes().isSameType(expectedValueType, getMethod().getReturnType())) {
                    return false;
                }
            }

            // verify that the chosen property implementations are supported
            switch (propertyMethod) {
            case "put":
                // must have XXX(int, K, V) -> V method
                return context.hasMethod(property.getPropertyImplementation(), propertyMethod, getValueType(),
                                         context.getTypes().getPrimitiveType(TypeKind.INT), getKeyType(),
                                         getValueType());
            case "get":
            case "remove":
                if (isMap) {
                    // must have XXX(int, K) -> V method
                    return context.hasMethod(property.getPropertyImplementation(), propertyMethod,
                                             getValueType(),
                                             context.getTypes().getPrimitiveType(TypeKind.INT), getKeyType());
                } // else fall through
            case "contains":
            case "add":
                // must have a XXX(int, E) -> boolean method where E is the element type stored by the property type
                return context.hasMethod(property.getPropertyImplementation(), propertyMethod,
                                         context.getTypes().getPrimitiveType(TypeKind.BOOLEAN),
                                         context.getTypes().getPrimitiveType(TypeKind.INT), getKeyType());
            default:
                return false;
            }
        }

        @Override
        public String getAnnotationSyntax(Context context, PropertyDeclaration property,
                                          TypeMirror attribute) {
            String fromMethod = getMethodAnnotationReflectionSyntax(context, attribute);
            if (fromMethod == null) {
                return getMethodParameterAnnotationReflectionSyntax(context, 0, attribute);
            } else {
                return fromMethod;
            }
        }

        @Override
        public void appendMembers(Generator generator) {
            // needs no additional members
        }

        @Override
        public void appendConstructorInitialization(Generator generator) {
            // needs no additional initialization
        }

        @Override
        public void appendMethodBody(Generator generator) {
            // perform any validation
            boolean needsUpdate = mutator;
            for (Annotation annot : getAttributes()) {
                if (annot instanceof Validate) {
                    Validations.appendValidation(getParameterNames(), (Validate) annot, generator);
                } else if (annot instanceof DoNotAutoVersion) {
                    // this is effectively ignored if mutator starts out false
                    needsUpdate = false;
                }
            }

            Context ctx = generator.getContext();
            String name = getParameterNames().get(0);
            for (AnnotationMirror annot : property.getAttributes()) {
                if (generator.getContext()
                             .isAnnotationType(annot, com.lhkbob.entreri.property.Collection.class)) {
                    if (propertyMethod.equals("put")) {
                        // validate both key and value
                        Validations.appendElementNotNull(name, ctx.asAnnotation(annot,
                                                                                com.lhkbob.entreri.property.Collection.class),
                                                         generator);
                        Validations.appendElementNotNull(getParameterNames().get(1), ctx.asAnnotation(annot,
                                                                                                      com.lhkbob.entreri.property.Collection.class),
                                                         generator);
                    } else {
                        Validations.appendElementNotNull(name, ctx.asAnnotation(annot,
                                                                                com.lhkbob.entreri.property.Collection.class),
                                                         generator);
                    }
                }
                // else ignore the unsupported attribute
            }

            String field = generator.getPropertyMemberName(property.getName());
            String index = generator.getComponentIndex();
            String returnStatement = null;
            if (isMapProperty() && !propertyMethod.equals("contains")) {
                // the result of all methods but contains is the value type
                StringBuilder stm = new StringBuilder();
                stm.append(getValueType()).append(" result = ").append(field).append(".")
                   .append(propertyMethod).append("(").append(index);
                for (String param : getParameterNames()) {
                    stm.append(", ").append(param);
                }
                stm.append(");");
                generator.appendSyntax(stm.toString());

                if (getMethod().getReturnType().getKind() == TypeKind.BOOLEAN) {
                    returnStatement = "return result != null;";
                } else if (getMethod().getReturnType().getKind() != TypeKind.VOID) {
                    // for map types, the return value is always the result of the property (don't need to worry about return this;)
                    returnStatement = "return result;";
                }
            } else {
                generator.appendSyntax("boolean result = " + field + "." + propertyMethod + "(" + index +
                                       ", " +
                                       getParameterNames().get(0) + ");");
                // the return type can be void, boolean or the component type
                if (getMethod().getReturnType().getKind() == TypeKind.BOOLEAN) {
                    returnStatement = "return result;";
                } else if (getMethod().getReturnType().getKind() != TypeKind.VOID) {
                    returnStatement = "return this;";
                }
            }

            if (needsUpdate) {
                generator.appendSyntax("updateVersion();");
            }

            if (returnStatement != null) {
                generator.appendSyntax(returnStatement);
            }
        }

        @Override
        protected Collection<PropertyDeclaration> getProperties() {
            return Collections.singleton(property);
        }
    }
}
