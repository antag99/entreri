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
package com.lhkbob.entreri.impl;

import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.property.ObjectProperty;

import javax.lang.model.SourceVersion;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ComponentFactoryProvider provides Factory instances that can create instances of
 * components on demand.  The provider encapsulates the strategy used to create or
 * generate the component implementations. The two current approaches are to generate the
 * source at build time and look them up using reflection, and to use Janino to
 * dynamically generate and compile classes.
 *
 * @author Michael Ludwig
 */
public abstract class ComponentFactoryProvider {
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
        public ComponentSpecification getSpecification();
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
     * @param spec           The component specification
     * @param includePackage True if the package should be included
     *
     * @return The class name that corresponds to the generated proxy implmentation for
     *         the given type
     */
    public static String getImplementationClassName(ComponentSpecification spec,
                                                    boolean includePackage) {
        // first get the simple name, concatenating all types in the hierarchy
        // (e.g. if its an inner class the name is Foo.Blah and this converts it to FooBlah)
        String scrubbed = spec.getType().replace("[\\.]", "");

        // compute a unique hash on the original canonical class name to guarantee
        // the uniqueness of the generated class as well
        int hash;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update((spec.getPackage() + "." + spec.getType()).getBytes(CHARSET));

            ByteBuffer md5 = ByteBuffer.wrap(md.digest());
            hash = Math.abs(md5.getInt() ^ md5.getInt() ^ md5.getInt() ^ md5.getInt());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("JVM does not support MD5", e);
        }

        StringBuilder sb = new StringBuilder();
        if (includePackage && !spec.getPackage().isEmpty()) {
            sb.append(spec.getPackage()).append('.');
        }
        sb.append(scrubbed).append("Impl").append(hash);

        return sb.toString();
    }

    private static final Charset CHARSET = Charset.forName("UTF-8");

    /**
     * Generate valid Java source code for a proxy implementation of the given component
     * type. The name and package of the generated class are consistent with the results
     * of calling {@link #getImplementationClassName(ComponentSpecification, boolean)}. It
     * is assumed (and not validated) that the property specification is valid and
     * corresponds to the component type. The new class will also extend from
     * AbstractComponent and has a single constructor that takes a ComponentRepository.
     * <p/>
     * The target source version generates two different outputs based on whether or not
     * it should take advantage of post 1.5 features.
     *
     * @param spec          The component specification that must be implemented
     * @param targetVersion The Java source version to target
     *
     * @return Source code of a valid implementation for the component type
     */
    public static String generateJavaCode(ComponentSpecification spec,
                                          SourceVersion targetVersion) {
        boolean use15 = targetVersion.compareTo(SourceVersion.RELEASE_5) >= 0;
        String implName = getImplementationClassName(spec, false);

        // the implementation will extend AbstractComponent sans generics because
        // Janino does not support them right now
        StringBuilder sb = new StringBuilder();

        if (!spec.getPackage().isEmpty()) {
            sb.append("package ").append(spec.getPackage()).append(";\n\n");
        }

        if (use15) {
            // prepend some annotations (and get UTC formatted date, as required by
            // the @Generated annotation if we attach a date, which we do because it's useful)
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
            df.setTimeZone(tz);
            String nowAsISO = df.format(new Date());

            sb.append("import javax.annotation.Generated;\n\n");
            sb.append("@Generated(value={\"")
              .append(ComponentFactoryProvider.class.getCanonicalName())
              .append("\"}, date=\"").append(nowAsISO).append("\")\n");
            sb.append("@SuppressWarnings(\"unchecked\")\n");
        }

        sb.append("public class ").append(implName).append(" extends ")
          .append(ABSTRACT_COMPONENT_NAME);
        if (use15) {
            sb.append('<').append(spec.getType()).append('>');
        }
        sb.append(" implements ").append(spec.getType()).append(" {\n");

        // add property instances with proper cast so we don't have to do that every
        // time a property is accessed, and add any shared instance field declarations
        int property = 0;
        for (PropertyDeclaration s : spec.getProperties()) {
            sb.append("\tprivate final ").append(s.getPropertyImplementation())
              .append(' ').append(PROPERTY_FIELD_PREFIX).append(property).append(";\n");
            if (s.isShared()) {
                sb.append("\tprivate final ").append(s.getType()).append(' ')
                  .append(SHARED_FIELD_PREFIX).append(property).append(";\n");
            }
            property++;
        }

        // define the constructor, must invoke super, assign properties, and allocate
        // shared instances; as with type declaration we cannot use generics
        sb.append("\n\tpublic ").append(implName).append("(").append(COMPONENT_REPO_NAME);
        if (use15) {
            sb.append('<').append(spec.getType()).append('>');
        }

        sb.append(" ").append(REPO_FIELD_NAME).append(") {\n").append("\t\tsuper(")
          .append(REPO_FIELD_NAME).append(");\n");
        property = 0;
        for (PropertyDeclaration s : spec.getProperties()) {
            sb.append("\t\t").append(PROPERTY_FIELD_PREFIX).append(property)
              .append(" = (").append(s.getPropertyImplementation()).append(") ")
              .append(REPO_FIELD_NAME).append('.').append(GET_PROPERTY_METHOD).append('(')
              .append(property).append(");\n");
            if (s.isShared()) {
                sb.append("\t\t").append(SHARED_FIELD_PREFIX).append(property)
                  .append(" = ").append(PROPERTY_FIELD_PREFIX).append(property)
                  .append('.').append(CREATE_SHARE_METHOD).append("();\n");
            }
            property++;
        }
        sb.append("\t}\n");

        // implement all getters of the interface, and accumulate setters
        Map<String, List<PropertyDeclaration>> setters = new HashMap<>();
        property = 0;
        for (PropertyDeclaration s : spec.getProperties()) {
            if (use15) {
                sb.append("\n\t@Override");
            }
            appendGetter(s, property, sb, use15);
            List<PropertyDeclaration> setterParams = setters.get(s.getSetterMethod());
            if (setterParams == null) {
                setterParams = new ArrayList<>();
                setters.put(s.getSetterMethod(), setterParams);
            }
            setterParams.add(s);

            property++;
        }

        // implement all setters
        for (List<PropertyDeclaration> setter : setters.values()) {
            if (use15) {
                sb.append("\n\t@Override");
            }
            appendSetter(setter, spec, sb);
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
    private static final String INDEX_FIELD_NAME = "index";
    private static final String GET_PROPERTY_METHOD = "getProperty";
    private static final String CREATE_SHARE_METHOD = "createShareableInstance";
    private static final String UPDATE_VERSION_METHOD = "incrementVersion";

    private static final String PROPERTY_FIELD_PREFIX = "property";
    private static final String SHARED_FIELD_PREFIX = "sharedInstance";
    private static final String SETTER_PARAM_PREFIX = "param";

    // Internal helper functions to generate the source code

    /**
     * Append the getter method definition for the given property.
     *
     * @param forProperty The property whose getter method will be defined
     * @param index       The index of the property in the overall spec
     * @param sb          The buffer to append to
     * @param useGenerics True if the source can use generics, in which case
     *                    ObjectProperties are properly parameterized
     */
    private static void appendGetter(PropertyDeclaration forProperty, int index,
                                     StringBuilder sb, boolean useGenerics) {
        // method signature
        sb.append("\n\tpublic ").append(forProperty.getType()).append(" ")
          .append(forProperty.getGetterMethod()).append("() {\n\t\t");

        // implementation body, depending on if we use a shared instance variable or not
        if (forProperty.isShared()) {
            sb.append(PROPERTY_FIELD_PREFIX).append(index).append(".get(")
              .append(INDEX_FIELD_NAME).append(", ").append(SHARED_FIELD_PREFIX)
              .append(index).append(");\n\t\treturn ").append(SHARED_FIELD_PREFIX)
              .append(index).append(";");
        } else {
            if (forProperty.getPropertyImplementation().equals(OBJECT_PROP_NAME)) {
                // special case where we allow ObjectProperty to have more permissive getters
                // and setters to support any type under the sun, but that means we have
                // to cast the object we get back
                sb.append("return (").append(forProperty.getType()).append(") ")
                  .append(PROPERTY_FIELD_PREFIX).append(index).append(".get(")
                  .append(INDEX_FIELD_NAME).append(");");
            } else {
                sb.append("return ").append(PROPERTY_FIELD_PREFIX).append(index)
                  .append(".get(").append(INDEX_FIELD_NAME).append(");");
            }
        }

        sb.append("\n\t}\n");
    }

    /**
     * Append the setter method definition given the property declarations that correspond
     * to the method and its parameters.
     *
     * @param params The properties mutated and that define the parameters of the setter
     *               method
     * @param spec   The spec for the component type being generated
     * @param sb     The buffer to append to
     */
    private static void appendSetter(List<PropertyDeclaration> params,
                                     ComponentSpecification spec, StringBuilder sb) {
        // order by parameter index
        Collections.sort(params, new Comparator<PropertyDeclaration>() {
            @Override
            public int compare(PropertyDeclaration o1, PropertyDeclaration o2) {
                return o1.getSetterParameter() - o2.getSetterParameter();
            }
        });

        List<? extends PropertyDeclaration> properties = spec.getProperties();
        String name = params.get(0).getSetterMethod();
        boolean returnComponent = params.get(0).getSetterReturnsComponent();

        // complete method signature
        if (returnComponent) {
            sb.append("\n\tpublic ").append(spec.getType());
        } else {
            sb.append("\n\tpublic void");
        }
        sb.append(' ').append(name).append('(');

        // with its possibly many parameters
        boolean first = true;
        for (PropertyDeclaration p : params) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(p.getType()).append(' ').append(SETTER_PARAM_PREFIX)
              .append(properties.indexOf(p));
        }
        sb.append(") {\n");

        // implement the body
        for (PropertyDeclaration p : params) {
            int idx = properties.indexOf(p);
            sb.append("\t\t").append(PROPERTY_FIELD_PREFIX).append(idx).append(".set(")
              .append(INDEX_FIELD_NAME).append(", ").append(SETTER_PARAM_PREFIX)
              .append(idx).append(");\n");
        }

        sb.append("\t\t").append(REPO_FIELD_NAME).append('.')
          .append(UPDATE_VERSION_METHOD).append("(").append(INDEX_FIELD_NAME)
          .append(");\n");

        // return this component if we're not a void setter
        if (returnComponent) {
            sb.append("\t\treturn this;\n");
        }
        sb.append("\t}\n");
    }
}
