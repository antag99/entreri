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

import com.lhkbob.entreri.IllegalComponentDefinitionException;
import com.lhkbob.entreri.property.Attribute;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PropertyDeclaration
 * ===================
 *
 * PropertyDeclaration represents a particular "property" instance declared in a Component sub-interface. A
 * property is represented by a bean getter method and an associated setter. This interface captures the
 * requisite information needed to implement a component type.
 *
 * @author Michael Ludwig
 */
public class PropertyDeclaration implements Comparable<PropertyDeclaration> {
    private final Context context;
    private final String name;
    private final TypeMirror type;

    private TypeMirror propertyImpl;
    private ExecutableElement propertyConstructor;

    private final Set<AnnotationMirror> attrs;
    private final Set<MethodDeclaration> methods;

    /**
     * Create a new PropertyDeclaration within the given `context`, that will have the provided `name` and
     * logical `type`. This will not have a selected property implementation yet.
     *
     * @param context The context of the component generation
     * @param name    The name of the property
     * @param type    The logical type of the property (e.g. what's exposed by the component type)
     */
    public PropertyDeclaration(Context context, String name, TypeMirror type) {
        this(context, name, type, null);
    }

    /**
     * Create a new PropertyDeclaration within the given `context`, that will have the provided `name` and
     * logical `type`. The property implementation will be initialized to `propertyImpl`, which may throw
     * an exception if the implementation is invalid.
     *
     * @param context      The context of the component generation
     * @param name         The name of the property
     * @param type         The logical type of the property (e.g. what's exposed by the component type)
     * @param propertyImpl The chosen Property implementation supporting `type`
     */
    public PropertyDeclaration(Context context, String name, TypeMirror type, TypeMirror propertyImpl) {
        this.context = context;
        this.name = name;
        this.type = type;

        attrs = new HashSet<>();
        methods = new HashSet<>();

        setPropertyImplementation(propertyImpl);
    }

    /**
     * Update this declaration to use the chosen Property implementation. This will throw an exception if
     * the given Property type does not define a constructor the declaration knows how to support.
     *
     * @param propertyImpl The property class that supports this logical property type
     */
    public void setPropertyImplementation(TypeMirror propertyImpl) {
        this.propertyImpl = propertyImpl;
        if (propertyImpl != null) {
            propertyConstructor = validateConstructor(context, name, propertyImpl);
        } else {
            propertyConstructor = null;
        }
    }

    private static ExecutableElement validateConstructor(Context context, String name,
                                                         TypeMirror propertyImpl) {
        ExecutableElement ctor = getValidConstructor(context, context.asElement(propertyImpl));

        if (ctor == null) {
            throw new IllegalComponentDefinitionException("Property chosen for " + name +
                                                          " has no valid constructor: " + propertyImpl);
        }
        return ctor;
    }

    public static ExecutableElement getValidConstructor(TypeUtils typeUtils, TypeElement propertyType) {
        List<? extends ExecutableElement> ctors = ElementFilter.constructorsIn(typeUtils.getElements()
                                                                                        .getAllMembers(propertyType));

        ExecutableElement longestValid = null;
        for (ExecutableElement ctor : ctors) {
            if (isValidConstructor(typeUtils, ctor)) {
                if (longestValid == null ||
                    ctor.getParameters().size() > longestValid.getParameters().size()) {
                    longestValid = ctor;
                }
            }
        }

        return longestValid;
    }

    private static boolean isValidConstructor(TypeUtils typeUtils, ExecutableElement ctor) {
        // constructor can't have any type parameters
        if (!ctor.getTypeParameters().isEmpty()) {
            return false;
        }

        List<? extends VariableElement> args = ctor.getParameters();
        if (args.size() == 0) {
            return true;
        }

        Types tu = typeUtils.getTypes();

        int startIndex = 0;
        if (tu.isSameType(tu.erasure(args.get(0).asType()), typeUtils.getRawType(Class.class))) {
            startIndex = 1;
        }

        TypeMirror annotClass = typeUtils.fromClass(Annotation.class);
        for (int i = startIndex; i < args.size(); i++) {
            if (!typeUtils.getTypes().isAssignable(args.get(i).asType(), annotClass)) {
                return false;
            }

            Attribute attr = typeUtils.asElement(args.get(i).asType()).getAnnotation(Attribute.class);
            if (attr == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return The chosen constructor of the Property implementation, or null if the implementation hasn't
     * been determined yet
     */
    public ExecutableElement getPropertyConstructor() {
        return propertyConstructor;
    }

    /**
     * @return Valid Java syntax that would create a new Property instance of the selected implementation,
     * that looks up all required annotation attributes for the constructor
     */
    public String getConstructorInvocationSyntax() {
        StringBuilder sb = new StringBuilder();
        sb.append("new ").append(propertyImpl).append("(");
        Types tu = context.getTypes();

        boolean first = true;
        for (VariableElement p : propertyConstructor.getParameters()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            TypeMirror argType = p.asType();
            if (tu.isSameType(tu.erasure(argType), tu.erasure(context.fromClass(Class.class)))) {
                sb.append(type).append(".class");
            } else {
                // assume it's an attribute annotation
                String getAnnot = null;
                for (MethodDeclaration m : methods) {
                    getAnnot = m.getAnnotationSyntax(context, this, argType);
                    if (getAnnot != null) {
                        break;
                    }
                }
                sb.append(getAnnot);
            }
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Get the logical name of the property, such as the name extracted from the getter bean method, or from
     * the {@link com.lhkbob.entreri.Named} annotation.
     *
     * @return The property name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the  of the type of value stored by this property.
     *
     * @return The type of this property, suitable for inclusion in source code
     */
    public TypeMirror getType() {
        return type;
    }

    /**
     * Get the canonical class name of the {@link com.lhkbob.entreri.property.Property Property}
     * implementation corresponding to the type of this property.
     *
     * @return The property implementation, suitable for inclusion in source code
     */
    public TypeMirror getPropertyImplementation() {
        return propertyImpl;
    }

    /**
     * Get all methods of the component that interact with this property. These methods may be bean-like
     * getters and setters, methods that manipulate or access elements within the property value (e.g.
     * collections), or bulk setters that are shared by multiple properties.
     *
     * @return All methods that use this property
     */
    public Set<MethodDeclaration> getMethods() {
        return methods;
    }

    /**
     * Get all property-level attribute annotations applied to any valid entry point of the property.
     *
     * @return All annotations present on the given parameter
     */
    public Set<AnnotationMirror> getAttributes() {
        return attrs;
    }

    @Override
    public int compareTo(PropertyDeclaration prop) {
        return name.compareTo(prop.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PropertyDeclaration)) {
            return false;
        }
        PropertyDeclaration p = (PropertyDeclaration) o;
        return name.equals(p.name) && type.equals(p.type) &&
               (propertyImpl == null ? p.propertyImpl == null : propertyImpl.equals(p.propertyImpl));
    }

    @Override
    public int hashCode() {
        int result = 17;
        result *= 31 * result + name.hashCode();
        result *= 31 * result + type.hashCode();
        result *= 31 * result + (propertyImpl == null ? 0 : propertyImpl.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" ").append(type);
        if (propertyImpl != null) {
            sb.append(" [implemented by ").append(propertyImpl).append("]");
        }
        sb.append("\n\t[methods ");
        boolean first = true;
        for (MethodDeclaration m : methods) {
            if (first) {
                first = false;
            } else {
                sb.append(",\n\t\t");
            }
            String[] ms = m.toString().split("\\n");
            boolean firstMethod = true;
            for (String s : ms) {
                if (firstMethod) {
                    firstMethod = false;
                } else {
                    sb.append("\n\t\t");
                }
                sb.append(s);
            }
        }
        sb.append("]\n\t[attrs ");
        first = true;
        for (AnnotationMirror attr : attrs) {
            if (first) {
                first = false;
            } else {
                sb.append(",\n\t\t");
            }
            sb.append(attr);
        }
        sb.append("]");
        return sb.toString();
    }
}
