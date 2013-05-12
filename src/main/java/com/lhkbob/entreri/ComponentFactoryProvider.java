/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2012, Michael Ludwig
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
package com.lhkbob.entreri;

import com.lhkbob.entreri.property.ObjectProperty;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ComponentFactoryProvider provides Factory instances that can create instances of
 * components on demand.  The provider encapsulates the strategy used to create or
 * generate the component implementations. The two current approaches are to generate the
 * source at build time and look them up using reflection, and to use Janino to
 * dynamically generate and compile classes.
 *
 * @author Michael Ludwig
 */
abstract class ComponentFactoryProvider {
    /**
     * Factory is a factory specification for creating proxy implementations of a
     * particular component interface. Additionally, to be compatible with the underlying
     * details of Entreri, all implementations must be AbstractComponents in addition to
     * implementing whatever required component type.
     *
     * @param <T> The component interface all created interfaces implement from this
     *            factory
     */
    public static interface Factory<T extends Component> {
        /**
         * Create a new instance that will be managed by the given ComponentRepository.
         * Depending on how it's used by the repository, it may or may not be flyweight.
         * The created component must be an AbstractComponent, and be cast-able to an
         * instance of T.
         *
         * @param forRepository The repository using the instance
         *
         * @return A new component instance
         */
        public AbstractComponent<T> newInstance(ComponentRepository<T> forRepository);

        /**
         * Get the property specification used by this factory. The list will be immutable
         * and not change during runtime.  The ordering must be consistent and ordered by
         * PropertySpecification's natural ordering.
         *
         * @return The property specification used by the factory
         */
        public List<PropertySpecification> getSpecification();
    }

    /**
     * Create or lookup the factory capable of creating the given component type. In many
     * cases the factory will have a comparatively expensive creation time and should be
     * cached and reused to mitigate this cost. Actual instance creation will likely be as
     * cheap as a reflection-based constructor invocation.
     * <p/>
     * This method is thread-safe and will guarantee there is at most a single factory
     * instance for each type. However, the {@link CachingDelegatingFactoryProvider}
     * implements this logic so that actual factory providers can have a simpler
     * implementation (where it is acceptable to allocate a new factory when their
     * getFactory() method is called).
     *
     * @param componentType The component type of the returned factory
     * @param <T>           The component type
     *
     * @return The unique factory for the given component type from this provider
     */
    public abstract <T extends Component> Factory<T> getFactory(Class<T> componentType);

    /**
     * Get the singleton thread-safe factory provider that can be used to create factories
     * for each component type.  This is intended for internal use only.
     *
     * @return The singleton provider
     */
    public static ComponentFactoryProvider getInstance() {
        return INSTANCE;
    }

    private static final ComponentFactoryProvider INSTANCE = new CachingDelegatingFactoryProvider();

    /**
     * Get the unique implementation name that should be used when generating source files
     * or looking for an existing proxy class that implements the given component type. If
     * <var>includePackage</var> is true, the returned string will include the package
     * name to create a valid, absolute type name.
     *
     * @param type           The component type
     * @param includePackage True if the package should be included
     *
     * @return The class name that corresponds to the generated proxy implmentation for
     *         the given type
     */
    public static String getImplementationClassName(Class<? extends Component> type,
                                                    boolean includePackage) {
        // first get the simple name, concatenating all types in the hierarchy
        // (e.g. if its an inner class the name is Foo$Blah and this converts it to FooBlah)
        String scrubbed = type.getSimpleName().replace("[\\.$]", "");

        // compute a unique hash on the original canonical class name to guarantee
        // the uniqueness of the generated class as well
        int hash;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(type.getCanonicalName().getBytes(CHARSET));

            ByteBuffer md5 = ByteBuffer.wrap(md.digest());
            hash = Math.abs(md5.getInt() ^ md5.getInt() ^ md5.getInt() ^ md5.getInt());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("JVM does not support MD5", e);
        }

        StringBuilder sb = new StringBuilder();
        if (includePackage) {
            sb.append(type.getPackage().getName()).append('.');
        }
        sb.append(scrubbed).append("Impl").append(hash);

        return sb.toString();
    }

    private static final Charset CHARSET = Charset.forName("UTF-8");

    /**
     * Generate valid Java source code for a proxy implementation of the given component
     * type. The name and package of the generated class are consistent with the results
     * of calling {@link #getImplementationClassName(Class, boolean)}. It is assumed (and
     * not validated) that the property specification is valid and corresponds to the
     * component type. The new class will also extend from AbstractComponent and has a
     * single constructor that takes a ComponentRepository.
     * <p/>
     * If <var>useGenerics</var> is true, the source code will use generics for
     * AbstractComponent, its ComponentRepository, and any ObjectProperty instances
     * present. Otherwise, raw types and casts will be placed in the source. Generics are
     * not supported by Janino's runtime compiler.
     * <p/>
     * If not, undefined behavior can occur later on if the specification is inconsistent
     * with the interface declared by the component
     *
     * @param type        The component interface that must be implemented
     * @param spec        The corresponding property specification used to generate the
     *                    source code
     * @param useGenerics True if the generated source should take advantage of Java
     *                    generics
     *
     * @return Source code of a valid implementation for the component type
     */
    public static String generateJavaCode(Class<? extends Component> type,
                                          List<PropertySpecification> spec,
                                          boolean useGenerics) {
        String baseTypeName = safeName(type);

        String implName = getImplementationClassName(type, false);

        // the implementation will extend AbstractComponent sans generics because
        // Janino does not support them right now
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(type.getPackage().getName()).append(";\n\n")
          .append("public class ").append(implName).append(" extends ")
          .append(ABSTRACT_COMPONENT_NAME);
        if (useGenerics) {
            sb.append('<').append(baseTypeName).append('>');
        }
        sb.append(" implements ").append(baseTypeName).append(" {\n");

        // add property instances with proper cast so we don't have to do that every
        // time a property is accessed, and add any shared instance field declarations
        int property = 0;
        for (PropertySpecification s : spec) {
            String fld = (useGenerics && s.getPropertyType().equals(ObjectProperty.class)
                          ? OBJECT_PROP_NAME + "<" + baseTypeName + ">"
                          : safeName(s.getPropertyType()));

            sb.append("\tprivate final ").append(fld).append(' ')
              .append(PROPERTY_FIELD_PREFIX).append(property).append(";\n");
            if (s.isSharedInstance()) {
                sb.append("\tprivate final ").append(safeName(s.getType())).append(' ')
                  .append(SHARED_FIELD_PREFIX).append(property).append(";\n");
            }
            property++;
        }

        // define the constructor, must invoke super, assign properties, and allocate
        // shared instances; as with type declaration we cannot use generics
        sb.append("\n\tpublic ").append(implName).append("(").append(COMPONENT_REPO_NAME);
        if (useGenerics) {
            sb.append('<').append(baseTypeName).append('>');
        }

        sb.append(" ").append(REPO_FIELD_NAME).append(") {\n").append("\t\tsuper(")
          .append(REPO_FIELD_NAME).append(");\n");
        property = 0;
        for (PropertySpecification s : spec) {
            String cast = (useGenerics && s.getPropertyType().equals(ObjectProperty.class)
                           ? OBJECT_PROP_NAME + "<" + baseTypeName + ">"
                           : safeName(s.getPropertyType()));

            sb.append("\t\t").append(PROPERTY_FIELD_PREFIX).append(property)
              .append(" = (").append(cast).append(") ").append(REPO_FIELD_NAME)
              .append('.').append(GET_PROPERTY_METHOD).append('(').append(property)
              .append(");\n");
            if (s.isSharedInstance()) {
                sb.append("\t\t").append(SHARED_FIELD_PREFIX).append(property)
                  .append(" = ").append(PROPERTY_FIELD_PREFIX).append(property)
                  .append('.').append(CREATE_SHARE_METHOD).append("();\n");
            }
            property++;
        }
        sb.append("\t}\n");

        // implement all methods of the interface
        for (Method m : type.getMethods()) {
            // skip the same methods skipped by the property specification
            if (m.getDeclaringClass().equals(Component.class) ||
                m.getDeclaringClass().equals(Owner.class) ||
                m.getDeclaringClass().equals(Ownable.class)) {
                continue;
            }

            if (!appendGetter(m, spec, sb, useGenerics)) {
                if (!appendSetter(m, spec, sb)) {
                    throw new IllegalStateException(
                            "Unexpected method during code generation: " + m);
                }
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    // magic constants used to produce the component implementation source files
    private static final String ABSTRACT_COMPONENT_NAME = AbstractComponent.class
            .getName();
    private static final String COMPONENT_REPO_NAME = ComponentRepository.class.getName();
    private static final String OBJECT_PROP_NAME = ObjectProperty.class.getName();

    private static final String REPO_FIELD_NAME = "owner";
    private static final String INDEX_FIELD_NAME = "getIndex()";
    private static final String GET_PROPERTY_METHOD = "getProperty";
    private static final String CREATE_SHARE_METHOD = "createShareableInstance";
    private static final String UPDATE_VERSION_METHOD = "incrementVersion";

    private static final String PROPERTY_FIELD_PREFIX = "property";
    private static final String SHARED_FIELD_PREFIX = "sharedInstance";
    private static final String SETTER_PARAM_PREFIX = "param";

    // Internal helper functions to generate the source code

    /**
     * @param type The type whose name is returned after filtering
     *
     * @return A String version of the class name, including package, that is safe to
     *         insert into Java source code
     */
    private static String safeName(Class<?> type) {
        return type.getName().replace('$', '.');
    }

    /**
     * Append the generated declaration and definition for the given getter method, using
     * the component specification in <var>spec</var>.
     *
     * @param getter      The setter method to append
     * @param spec        The spec for the component type being generated
     * @param sb          The buffer to append to
     * @param useGenerics True if the source can use generics, in which case
     *                    ObjectProperties are properly parameterized
     *
     * @return True if the method was a valid getter, or false if it wasn't and no body
     *         was appended
     */
    private static boolean appendGetter(Method getter, List<PropertySpecification> spec,
                                        StringBuilder sb, boolean useGenerics) {
        PropertySpecification forProperty = findPropertyForGetter(getter, spec);
        if (forProperty == null) {
            return false;
        }

        // method signature
        int idx = spec.indexOf(forProperty);
        sb.append("\n\tpublic ").append(safeName(forProperty.getType())).append(" ")
          .append(getter.getName()).append("() {\n\t\t");

        // implementation body, depending on if we use a shared instance variable or not
        if (forProperty.isSharedInstance()) {
            sb.append(PROPERTY_FIELD_PREFIX).append(idx).append(".get(")
              .append(INDEX_FIELD_NAME).append(", ").append(SHARED_FIELD_PREFIX)
              .append(idx).append(");\n\t\treturn ").append(SHARED_FIELD_PREFIX)
              .append(idx).append(";");
        } else {
            if (forProperty.getPropertyType().equals(ObjectProperty.class) &&
                !useGenerics) {
                // special case where we allow ObjectProperty to have more permissive getters
                // and setters to support any type under the sun, but that means we have
                // to cast the object we get back
                sb.append("return (").append(safeName(forProperty.getType())).append(") ")
                  .append(PROPERTY_FIELD_PREFIX).append(idx).append(".get(")
                  .append(INDEX_FIELD_NAME).append(");");
            } else {
                sb.append("return ").append(PROPERTY_FIELD_PREFIX).append(idx)
                  .append(".get(").append(INDEX_FIELD_NAME).append(");");
            }
        }

        sb.append("\n\t}\n");
        return true;
    }

    /**
     * Append the generated declaration and definition for the given setter method, using
     * the component specification in <var>spec</var>.
     *
     * @param setter The setter method to append
     * @param spec   The spec for the component type being generated
     * @param sb     The buffer to append to
     *
     * @return True if the method was a valid setter, or false if it wasn't and no body
     *         was appended
     */
    private static boolean appendSetter(Method setter, List<PropertySpecification> spec,
                                        StringBuilder sb) {
        List<PropertySpecification> params = findPropertiesForSetter(setter, spec);
        if (params == null) {
            return false;
        }

        boolean returnComponent = !setter.getReturnType().equals(void.class);

        // complete method signature
        if (returnComponent) {
            sb.append("\n\tpublic ").append(safeName(setter.getReturnType()));
        } else {
            sb.append("\n\tpublic void");
        }
        sb.append(' ').append(setter.getName()).append('(');

        // with its possibly many parameters
        boolean first = true;
        for (PropertySpecification p : params) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(safeName(p.getType())).append(' ').append(SETTER_PARAM_PREFIX)
              .append(spec.indexOf(p));
        }
        sb.append(") {\n");

        // implement the body
        for (PropertySpecification p : params) {
            int idx = spec.indexOf(p);
            sb.append("\t\t").append(PROPERTY_FIELD_PREFIX).append(idx).append(".set(")
              .append(SETTER_PARAM_PREFIX).append(idx).append(", ")
              .append(INDEX_FIELD_NAME).append(");\n");
        }

        sb.append("\t\t").append(REPO_FIELD_NAME).append('.')
          .append(UPDATE_VERSION_METHOD).append("(").append(INDEX_FIELD_NAME)
          .append(");\n");

        // return this component if we're not a void setter
        if (returnComponent) {
            sb.append("\t\treturn this;\n");
        }
        sb.append("\t}\n");
        return true;
    }

    /**
     * Get the property specification that matches the given getter method. The
     * specification is selected from the provided list, which is assumed to be the
     * complete spec.
     *
     * @param getter The getter method
     * @param spec   The complete specification for a type
     *
     * @return The matching property, or null if no property matches (i.e. it's not a
     *         valid getter method)
     */
    private static PropertySpecification findPropertyForGetter(Method getter,
                                                               List<PropertySpecification> spec) {
        for (PropertySpecification s : spec) {
            if (s.getGetterMethod().equals(getter)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Get every property specification from the given setter method. The specifications
     * will be selected from the provided list, which is assumed to be the complete spec.
     * The returned list will be ordered by the setter parameter of each property in the
     * provided method.
     *
     * @param setter The setter method
     * @param spec   The complete specification for a type
     *
     * @return The properties attached to the setter, ordered by their parameter index, or
     *         null if no properties match (i.e. it's not a valid setter method)
     */
    private static List<PropertySpecification> findPropertiesForSetter(Method setter,
                                                                       List<PropertySpecification> spec) {
        List<PropertySpecification> matches = new ArrayList<PropertySpecification>();
        for (PropertySpecification s : spec) {
            if (s.getSetterMethod().equals(setter)) {
                matches.add(s);
            }
        }

        if (matches.isEmpty()) {
            return null;
        } else {
            Collections.sort(matches, new Comparator<PropertySpecification>() {
                @Override
                public int compare(PropertySpecification o1, PropertySpecification o2) {
                    return o1.getSetterParameter() - o2.getSetterParameter();
                }
            });
            return matches;
        }
    }
}
