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

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.IllegalComponentDefinitionException;
import com.lhkbob.entreri.Ownable;
import com.lhkbob.entreri.Owner;
import com.lhkbob.entreri.attr.Attribute;
import com.lhkbob.entreri.attr.ImplementedBy;
import com.lhkbob.entreri.attr.Reference;
import com.lhkbob.entreri.property.Property;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * ComponentSpecification
 * ======================
 *
 * ComponentSpecification provides an interface to access the information encoded in a Component
 * sub-interface in order to generate a proxy implementation. This class is responsible for validating all
 * aspects of a Component sub-interface, determining the Property implementations to use, and matching all
 * declared methods with method patterns. Use this in conjunction with {@link
 * com.lhkbob.entreri.impl.apt.ComponentGenerator} to output actual Java source code for the component proxy
 * implementations.
 *
 * @author Michael Ludwig
 */
public class ComponentSpecification {
    private final TypeMirror componentType;
    private final String packageName;
    private final List<PropertyDeclaration> properties;
    private final List<MethodDeclaration> methods;

    /**
     * Create a new ComponentSpecification for the given Component subinterface `type`, operating within
     * the APT processing environment given by `env`. This uses the default list of method patterns with
     * precedence:
     *
     * 1. {@link com.lhkbob.entreri.impl.apt.MultiSetterPattern}
     * 2. {@link com.lhkbob.entreri.impl.apt.SharedBeanGetterPattern}
     * 3. {@link com.lhkbob.entreri.impl.apt.BeanGetterPattern}
     * 4. {@link com.lhkbob.entreri.impl.apt.BeanSetterPattern}
     *
     * @param type The component type to analyze
     * @param env  The processing environment
     */
    public ComponentSpecification(TypeElement type, ProcessingEnvironment env) {
        this(type, env, new MultiSetterPattern(), new SharedBeanGetterPattern(), new BeanGetterPattern(),
             new BeanSetterPattern());
    }

    /**
     * Create a new ComponentSpecification for the given Component sub-interface `type`, operating within
     * the APT processing environment given by `env`. This uses the provided list of method patterns
     * to match methods, where precedence is determined by the order provided.
     *
     * @param type     The component type to analyze
     * @param env      The processing environment
     * @param patterns The list of method patterns, with higher precedence first
     */
    public ComponentSpecification(TypeElement type, ProcessingEnvironment env, MethodPattern... patterns) {
        Context context = createContext(env, type.asType(), patterns);
        TypeMirror componentType = context.fromClass(Component.class);
        TypeMirror objectType = context.fromClass(Object.class);
        TypeMirror ownerType = context.fromClass(Owner.class);
        TypeMirror ownableType = context.fromClass(Ownable.class);

        if (!context.getTypes().isAssignable(type.asType(), componentType)) {
            throw fail(type.asType(), "Class must extend Component");
        }
        if (!type.getKind().equals(ElementKind.INTERFACE)) {
            throw fail(type.asType(), "Component definition must be an interface");
        }

        List<ExecutableElement> methods = filterMethods(context, ElementFilter.methodsIn(context.getElements()
                                                                                                .getAllMembers(type)),
                                                        componentType, objectType, ownerType, ownableType);

        List<PropertyDeclaration> properties = new ArrayList<>();
        for (MethodPattern pattern : patterns) {
            Map<ExecutableElement, Collection<? extends PropertyDeclaration>> matches = pattern.match(context,
                                                                                                      methods);
            for (ExecutableElement m : matches.keySet()) {
                properties.addAll(matches.get(m));
                methods.remove(m);
            }
        }

        if (!methods.isEmpty()) {
            throw fail(type.asType(), "Not all methods can be implemented: " + methods);
        }

        properties = compactProperties(context, properties);
        validateAttributes(context, properties);
        assignPropertyImplementations(context, properties);
        Collections.sort(properties);

        // accumulate methods
        Set<MethodDeclaration> allMethods = new HashSet<>();
        for (PropertyDeclaration p : properties) {
            allMethods.addAll(p.getMethods());
        }
        List<MethodDeclaration> orderedMethods = new ArrayList<>(allMethods);
        Collections.sort(orderedMethods);
        validateMethods(context, orderedMethods);

        context.getLogger().printMessage(Diagnostic.Kind.OTHER,
                                         getDebugSpecificationMessage(context.getComponentType(),
                                                                      properties));

        this.properties = Collections.unmodifiableList(properties);
        this.methods = Collections.unmodifiableList(orderedMethods);
        this.componentType = context.getComponentType();
        packageName = context.getElements().getPackageOf(type).getQualifiedName().toString();
    }

    private static String getDebugSpecificationMessage(TypeMirror componentType,
                                                       List<PropertyDeclaration> properties) {
        StringBuilder sb = new StringBuilder();
        sb.append("Properties for ").append(componentType).append(":\n");
        for (PropertyDeclaration p : properties) {
            sb.append("\t").append(p).append("\n");
        }
        return sb.toString();
    }

    private static IllegalComponentDefinitionException fail(TypeMirror type, String msg) {
        return new IllegalComponentDefinitionException(type.toString(), msg);
    }

    private static void validateAttributes(Context context, List<PropertyDeclaration> properties) {
        for (PropertyDeclaration p : properties) {
            Set<Class<? extends Annotation>> seen = new HashSet<>();
            for (Annotation a : p.getAttributes()) {
                if (!seen.add(a.annotationType())) {
                    throw fail(context.getComponentType(),
                               p.getName() + " has multiple distinct attribute values for type " +
                               a.annotationType());
                }
            }
        }
    }

    private static void validateMethods(Context context, List<MethodDeclaration> methods) {
        for (MethodDeclaration m : methods) {
            if (!m.arePropertiesValid(context)) {
                throw new IllegalComponentDefinitionException("Method pattern does not support selected property implementations: " +
                                                              m.getMethod());
            }
        }
    }

    private static List<PropertyDeclaration> assignPropertyImplementations(Context context,
                                                                           List<PropertyDeclaration> properties) {
        for (PropertyDeclaration p : properties) {
            if (p.getPropertyImplementation() == null) {
                TypeMirror propType = null;

                for (Annotation a : p.getAttributes()) {
                    if (a instanceof ImplementedBy) {
                        try {
                            ((ImplementedBy) a).value();
                        } catch (MirroredTypeException te) {
                            propType = te.getTypeMirror();
                        }
                        break;
                    }
                }

                if (propType == null) {
                    // look up from the file mapping after determining semantics
                    boolean useReferenceSemantics = false;
                    for (Annotation a : p.getAttributes()) {
                        if (a instanceof Reference) {
                            useReferenceSemantics = true;
                            break;
                        }
                    }
                    TypePropertyMapper mapper = (useReferenceSemantics ? context.getReferenceTypeMapper()
                                                                       : context.getValueTypeMapper());
                    propType = mapper.getPropertyFactory(p.getType());
                }

                p.setPropertyImplementation(propType);
            }

        }
        return properties;
    }

    private static List<PropertyDeclaration> compactProperties(Context context,
                                                               List<PropertyDeclaration> properties) {
        Map<String, PropertyDeclaration> compacted = new HashMap<>();
        for (PropertyDeclaration p : properties) {
            PropertyDeclaration compact = compacted.get(p.getName());
            if (compact == null) {
                // no compaction necessary yet
                compacted.put(p.getName(), p);
            } else {
                // make sure their types are compatible, and use the proper factory type (if specified)
                // and combine all of the attributes together
                if (!context.getTypes().isSameType(compact.getType(), p.getType())) {
                    throw fail(context.getComponentType(),
                               "Multiple methods create conflicting property type for " + p.getName());
                }

                if (p.getPropertyImplementation() != null) {
                    if (compact.getPropertyImplementation() != null && !context.getTypes()
                                                                               .isSameType(compact.getPropertyImplementation(),
                                                                                           p.getPropertyImplementation())) {
                        throw fail(context.getComponentType(),
                                   "Multiple methods use conflicting Property implementations for " +
                                   p.getName());
                    }
                    // swap them so that compact has the non-null property factory
                    PropertyDeclaration temp = p;
                    p = compact;
                    compact = temp;
                } // else compact may or may not have a non-null factory, but it will be preserved

                compact.getAttributes().addAll(p.getAttributes());
                for (MethodDeclaration m : p.getMethods()) {
                    m.replace(p, compact);
                    compact.getMethods().add(m);
                }
            }
        }

        return new ArrayList<>(compacted.values());
    }

    private static List<ExecutableElement> filterMethods(Context context, List<ExecutableElement> methods,
                                                         TypeMirror... unneeded) {
        List<ExecutableElement> filtered = new ArrayList<>();
        for (ExecutableElement m : methods) {
            TypeMirror declaredIn = Context.findEnclosingTypeElement(m).asType();
            boolean declaredInUnneeded = false;
            for (TypeMirror t : unneeded) {
                if (context.getTypes().isSameType(declaredIn, t)) {
                    declaredInUnneeded = true;
                    break;
                }
            }

            if (!declaredInUnneeded) {
                filtered.add(m);
            }
        }

        return filtered;
    }

    private static Context createContext(ProcessingEnvironment env, TypeMirror componentType,
                                         MethodPattern[] patterns) {
        Set<Class<? extends Annotation>> interested = new HashSet<>();
        // the currently supported influcencePropertyChoice = true attributes
        interested.add(ImplementedBy.class);
        interested.add(Reference.class);
        // any other attributes used by the patterns
        for (MethodPattern pattern : patterns) {
            interested.addAll(pattern.getSupportedAttributes());
        }
        // remove any that aren't actual attributes and output a debugging message
        Iterator<Class<? extends Annotation>> it = interested.iterator();
        while (it.hasNext()) {
            Class<? extends Annotation> attr = it.next();
            if (attr.getAnnotation(Attribute.class) == null) {
                it.remove();
                env.getMessager().printMessage(Diagnostic.Kind.WARNING, attr +
                                                                        " used as an attribute, but is not annotated with @Attribute");
            }
        }

        return new Context(env, componentType, new TypePropertyMapper(env, Property.ValueSemantics.class),
                           new TypePropertyMapper(env, Property.ReferenceSemantics.class), interested);
    }

    /**
     * Get the qualified name of the component type, including the package name reported by {@link
     * #getPackage()}. Thus, the returned string should be valid to insert into source code regardless of if
     * there's another property or type that may have the same class name.
     *
     * @return The component type
     */
    public TypeMirror getType() {
        return componentType;
    }

    /**
     * @return The package the component type resides in
     */
    public String getPackage() {
        return packageName;
    }

    /**
     * Get all properties that must be implemented for this component type. This will include all properties
     * defined in a parent component type if the type does not directly extend Component.
     *
     * The returned list will be immutable and sorted by logical property name.
     *
     * @return The list of all properties for the component
     */
    public List<PropertyDeclaration> getProperties() {
        return properties;
    }

    /**
     * Get all methods that must be implemented. This is the union of all methods from the property
     * declarations of this specification, with duplicates removed, and ordered by the method name.
     *
     * @return The list of methods the component type must implement to compile correctly
     */
    public List<? extends MethodDeclaration> getMethods() {
        return methods;
    }
}
