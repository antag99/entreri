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
import com.lhkbob.entreri.property.Property;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TypePropertyMapping
 * ===================
 *
 * TypePropertyMapping is an internal class used to maintain a thread-safe, shared, and consistent mapping
 * from Java type to an associated Property type that wraps that data. Primitive and plain Object wrapping is
 * built-in. A property can be overridden for a class by placing the file
 *
 * ```
 * META-INF/entreri/mapping/<CANONICAL CLASS NAME>-<semantics>;
 * ```
 *
 * in the class path, with a single string `BINARY CLASS NAME OF PROPERTY` as its contents. The class name
 * within the file must be a Property implementation that supports the class name of the file. The `semantics`
 * suffix must equal `value` for Property implementations extending {@link
 * com.lhkbob.entreri.property.Property.ValueSemantics}. The suffix must equal `reference` for those extending
 * {@link com.lhkbob.entreri.property.Property.ReferenceSemantics}.
 *
 * It is also capable of searching up a type's hierarchy to find a mapped property specified on a super
 * class and using it. This requires that there be no exact match for the requested type, and that the mapped
 * property for the super class be declared as generic and the generic type variable can unify with the
 * requested type.
 *
 * @author Michael Ludwig
 */
public final class TypePropertyMapper {
    public static final String MAPPING_DIR = "META-INF/entreri/mapping/";
    public static final String VALUE_SUFFIX = "value";
    public static final String REFERENCE_SUFFIX = "reference";

    private final ConcurrentHashMap<TypeMirror, DeclaredType> typeMapping;
    private final Types tu;
    private final Elements eu;
    private final Filer io;

    private final DeclaredType requiredSemantics;
    private final String pathSuffix;

    public TypePropertyMapper(ProcessingEnvironment env, Class<?> requiredSemantics) {
        if (requiredSemantics.equals(Property.ReferenceSemantics.class)) {
            pathSuffix = REFERENCE_SUFFIX;
        } else if (requiredSemantics.equals(Property.ValueSemantics.class)) {
            pathSuffix = VALUE_SUFFIX;
        } else {
            throw new IllegalArgumentException("Required semantics must be ValueSemantics or ReferenceSemantics, not: " +
                                               requiredSemantics);
        }
        tu = env.getTypeUtils();
        eu = env.getElementUtils();
        io = env.getFiler();

        typeMapping = new ConcurrentHashMap<>();
        this.requiredSemantics = (DeclaredType) eu.getTypeElement(requiredSemantics.getCanonicalName())
                                                  .asType();
    }

    /**
     * Attempt to determine a Property class that wraps the corresponding Java type.
     *
     * @param baseType The base type the property must support
     * @return A DeclaredType that represents a subclass of PropertyFactory
     */
    public DeclaredType getPropertyFactory(TypeMirror baseType) {
        DeclaredType mappedPropertyType = typeMapping.get(baseType);
        if (mappedPropertyType != null) {
            return mappedPropertyType;
        }

        TypeMirror propertyType = tu.erasure(eu.getTypeElement(Property.class.getCanonicalName()).asType());
        TypeMirror generic = tu.erasure(eu.getTypeElement(Property.Generic.class.getCanonicalName())
                                          .asType());

        Queue<TypeMirror> lookup = new ArrayDeque<>();
        lookup.add(baseType);
        Set<TypeMirror> visited = new HashSet<>();

        boolean requiresUnification = false;
        while (!lookup.isEmpty()) {
            TypeMirror toLookup = lookup.poll();
            if (visited.contains(toLookup)) {
                continue;
            }
            visited.add(toLookup);

            // try to lookup the mapping from the META-INF directory using the current type
            FileObject mapping;
            try {
                mapping = io.getResource(StandardLocation.CLASS_PATH, "",
                                         MAPPING_DIR + tu.erasure(toLookup).toString() + "-" + pathSuffix);
            } catch (IOException e) {
                // if an IO is thrown here, it means it couldn't find the file
                mapping = null;
            }

            if (mapping != null) {
                try {
                    String content = mapping.getCharContent(true).toString().trim();
                    TypeMirror mappedType = eu.getTypeElement(content).asType();
                    if (!tu.isAssignable(mappedType, propertyType)) {
                        throw new IllegalComponentDefinitionException(baseType.toString(),
                                                                      "Type mapping must be a Property, not: " +
                                                                      mappedType.toString());
                    }
                    if (!tu.isAssignable(mappedType, requiredSemantics)) {
                        throw new IllegalComponentDefinitionException(baseType.toString(),
                                                                      "Mapped property type has incorrect semantics: " +
                                                                      mappedType.toString());
                    }

                    // check if this property type implements Generic, and if it does unify the type parameters
                    // to the specified base type
                    for (TypeMirror superClass : tu.directSupertypes(mappedType)) {
                        if (tu.isSameType(tu.erasure(superClass), generic)) {
                            Map<TypeVariable, ReferenceType> varMapping = unify(baseType,
                                                                                ((DeclaredType) superClass)
                                                                                        .getTypeArguments()
                                                                                        .get(0));
                            if (varMapping == null) {
                                throw new IllegalComponentDefinitionException(baseType.toString(),
                                                                              "Declared type does not unify with generic property: " +
                                                                              superClass.toString());
                            }

                            DeclaredType incomplete = (DeclaredType) mappedType;
                            List<? extends TypeMirror> vars = incomplete.getTypeArguments();
                            TypeMirror[] args = new TypeMirror[vars.size()];
                            for (int i = 0; i < args.length; i++) {
                                TypeVariable var = (TypeVariable) vars.get(i);
                                args[i] = varMapping.get(var);
                            }
                            mappedPropertyType = tu.getDeclaredType((TypeElement) incomplete.asElement(),
                                                                    args);
                            break;
                        }
                    }
                    if (mappedPropertyType == null) {
                        if (requiresUnification) {
                            // the mapped property wasn't generic, but it needs to be to support the subtype
                            break;
                        } else {
                            // assume it's a non-generic exact match for the base type
                            mappedPropertyType = (DeclaredType) mappedType;
                        }
                    }

                    typeMapping.put(baseType, mappedPropertyType);
                    return mappedPropertyType;
                } catch (IOException e) {
                    // if an IO is thrown here, however, it means errors accessing
                    // the file, which we can't recover from
                    throw new RuntimeException(e);
                }
            }

            // no mapping was found for this type, so add super types to the search and continue
            lookup.addAll(tu.directSupertypes(toLookup));
            // after the first run (using the exact type), generic unification is required for a property to match
            requiresUnification = true;
        }
        // if we got here there's no property to find
        throw new IllegalComponentDefinitionException(baseType.toString(),
                                                      "Could not determine Property implementation for type with " +
                                                      pathSuffix + " semantics");
    }

    private Map<TypeVariable, ReferenceType> unify(TypeMirror targetType, TypeMirror genericType) {
        if (genericType.getKind() == TypeKind.DECLARED) {
            // the raw types of target and generic must be equal
            if (!tu.isSameType(tu.erasure(targetType), tu.erasure(genericType))) {
                return null;
            }
            // must have the same number of type variables
            // NOTE if the erasure's are equal, then targetType must also be a DeclaredType
            List<? extends TypeMirror> targetParams = ((DeclaredType) targetType).getTypeArguments();
            List<? extends TypeMirror> genericParams = ((DeclaredType) genericType).getTypeArguments();
            if (targetParams.size() != genericParams.size()) {
                return null;
            }
            // each type variable must unify
            Map<TypeVariable, ReferenceType> unification = new HashMap<>();
            for (int i = 0; i < targetParams.size(); i++) {
                Map<TypeVariable, ReferenceType> paramResult = unify(targetParams.get(i),
                                                                     genericParams.get(i));
                if (paramResult == null) {
                    return null;
                } else {
                    unification.putAll(paramResult);
                }
            }
            return unification;
        } else if (genericType.getKind() == TypeKind.ARRAY) {
            // target type must be an array type
            if (targetType.getKind() != TypeKind.ARRAY) {
                return null;
            }
            // component types must unify
            return unify(((ArrayType) targetType).getComponentType(),
                         ((ArrayType) genericType).getComponentType());
        } else if (genericType.getKind() == TypeKind.TYPEVAR) {
            // target type must be a reference type that's not a type var
            if (targetType.getKind() != TypeKind.DECLARED && targetType.getKind() != TypeKind.ARRAY) {
                return null;
            }
            Map<TypeVariable, ReferenceType> map = new HashMap<>();
            map.put((TypeVariable) genericType, (ReferenceType) targetType);
            return map;
        } else {
            // unexpected generic type
            return null;
        }
    }
}
