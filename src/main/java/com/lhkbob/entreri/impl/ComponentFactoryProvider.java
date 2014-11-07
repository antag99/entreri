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
package com.lhkbob.entreri.impl;

import com.lhkbob.entreri.Component;

import javax.lang.model.SourceVersion;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ComponentFactoryProvider
 * ========================
 *
 * ComponentFactoryProvider provides {@link com.lhkbob.entreri.impl.ComponentFactoryProvider.Factory}
 * instances that can create instances of components on demand.  The provider encapsulates the strategy used
 * to create or generate the component implementations. The current approach are to generate the source
 * at build time and look them up using reflection. It is relatively easy to extend this to an additional
 * Janino or Proxy factory if the APT based approach is insufficient.
 *
 * @author Michael Ludwig
 */
public abstract class ComponentFactoryProvider {
    /**
     * Factory is a factory specification for creating proxy implementations of a particular component
     * interface. Additionally, to be compatible with the underlying details of Entreri, all implementations
     * must be AbstractComponents in addition to implementing whatever required component type.
     *
     * @param <T> The component interface all created interfaces implement from this factory
     */
    public static interface Factory<T extends Component> {
        /**
         * Create a new instance that will be managed by the given ComponentRepository. Depending on how it's
         * used by the repository, it may or may not be flyweight. The created component must be an
         * AbstractComponent, and be cast-able to an instance of T.
         *
         * @param forRepository The repository using the instance
         * @return A new component instance
         */
        public AbstractComponent<T> newInstance(ComponentDataStore<T> forRepository);

        /**
         * Get the property specification used by this factory. The list will be immutable and not change
         * during runtime.  The ordering must be consistent and ordered by PropertySpecification's natural
         * ordering.
         *
         * @return The property specification used by the factory
         */
        public ComponentSpecification getSpecification();
    }

    /**
     * Create or lookup the factory capable of creating the given component type. In many cases the factory
     * will have a comparatively expensive creation time and should be cached and reused to mitigate this
     * cost. Actual instance creation will likely be as cheap as a reflection-based constructor invocation.
     *
     * This method is thread-safe and will guarantee there is at most a single factory instance for each type.
     * However, the {@link CachingDelegatingFactoryProvider} implements this logic so that actual factory
     * providers can have a simpler implementation (where it is acceptable to allocate a new factory when
     * their getFactory() method is called).
     *
     * @param componentType The component type of the returned factory
     * @return The unique factory for the given component type from this provider
     */
    public abstract <T extends Component> Factory<T> getFactory(Class<T> componentType);

    /**
     * Get the singleton thread-safe factory provider that can be used to create factories for each component
     * type.  This is intended for internal use only.
     *
     * @return The singleton provider
     */
    public static ComponentFactoryProvider getInstance() {
        return INSTANCE;
    }

    private static final ComponentFactoryProvider INSTANCE = new CachingDelegatingFactoryProvider();

    /**
     * Get the unique implementation name that should be used when generating source files or looking for an
     * existing proxy class that implements the given component type. If <var>includePackage</var> is true,
     * the returned string will include the package name to create a valid, absolute type name.
     *
     * @param spec           The component specification
     * @param includePackage True if the package should be included
     * @return The class name that corresponds to the generated proxy implementation for the given type
     */
    public static String getImplementationClassName(ComponentSpecification spec, boolean includePackage) {
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
     * Generate valid Java source code for a proxy implementation of the given component type. The name and
     * package of the generated class are consistent with the results of calling {@link
     * #getImplementationClassName(ComponentSpecification, boolean)}. It is assumed (and not validated) that
     * the property specification is valid and corresponds to the component type. The new class will also
     * extend from AbstractComponent and has a single constructor that takes a ComponentRepository.
     *
     * The target source version generates two different outputs based on whether or not it should take
     * advantage of 1.5+ features.
     *
     * @param spec          The component specification that must be implemented
     * @param targetVersion The Java source version to target
     * @return Source code of a valid implementation for the component type
     */
    public static String generateJavaCode(ComponentSpecification spec, SourceVersion targetVersion) {
        List<? extends MethodDeclaration> methods = spec.getMethods();

        // begin outputting source code
        final boolean use15 = targetVersion.compareTo(SourceVersion.RELEASE_5) >= 0;
        String implName = getImplementationClassName(spec, false);
        String genericParam = (use15 ? "<" + spec.getType() + ">" : "");

        GeneratorImpl generator = new GeneratorImpl();

        if (!spec.getPackage().isEmpty()) {
            generator.appendSyntax("package " + spec.getPackage() + ";");
            generator.newline();
        }

        if (use15) {
            // prepend some annotations (and get UTC formatted date, as required by
            // the @Generated annotation if we attach a date, which we do because it's useful)
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
            df.setTimeZone(tz);
            String nowAsISO = df.format(new Date());

            generator.appendSyntax("import javax.annotation.Generated;", "",
                                   "@Generated(value={\"" + ComponentFactoryProvider.class.getName() +
                                   "\"}, date=\"" + nowAsISO + "\")", "@SuppressWarnings(\"unchecked\")");
        }

        generator.appendSyntax("public class " + implName + " extends " + AbstractComponent.class.getName() +
                               genericParam + " implements " + spec.getType() + " {");
        generator.pushTab();

        // add property instances with proper cast so we don't have to do that every
        // time a property is accessed, and add any shared instance field declarations
        for (PropertyDeclaration s : spec.getProperties()) {
            generator.appendSyntax("private final " + s.getPropertyImplementation() + " " +
                                   generator.getPropertyMemberName(s.getName()) + ";");
        }
        generator.newline();
        for (MethodDeclaration m : methods) {
            m.appendMembers(generator);
        }
        generator.newline();

        // define the constructor, must invoke super, assign properties, and allocate
        // shared instances; as with type declaration we cannot use generics
        generator
                .appendSyntax("public " + implName + "(" + ComponentDataStore.class.getName() + genericParam +
                              " repo) {");
        generator.pushTab();
        generator.appendSyntax("super(repo);", "// get properties from data store");

        int propIndex = 0;
        for (PropertyDeclaration s : spec.getProperties()) {
            generator.appendSyntax(generator.getPropertyMemberName(s.getName()) + " = (" +
                                   s.getPropertyImplementation() + ") repo.getProperty(" + propIndex + ");");
            propIndex++;
        }
        generator.appendSyntax("// any extra member initialization");
        for (MethodDeclaration m : methods) {
            m.appendConstructorInitialization(generator);
        }
        generator.popTab();
        generator.appendSyntax("}", "");

        // now add all methods
        for (MethodDeclaration m : methods) {
            generator.appendMethod(m);
            generator.newline();
        }

        // close the class
        generator.popTab();
        generator.appendSyntax("}", "");
        return generator.getSource();
    }

    private static class GeneratorImpl implements Generator {
        private int tabCount;
        private final StringBuilder source;

        private final Map<Object, Map<String, String>> auxMembers;
        private int memberCounter;

        public GeneratorImpl() {
            source = new StringBuilder();
            tabCount = 0;
            memberCounter = 0;
            auxMembers = new HashMap<>();
        }

        public String getSource() {
            return source.toString();
        }

        public void pushTab() {
            tabCount++;
        }

        public void popTab() {
            tabCount = Math.max(0, tabCount - 1);
        }

        public void appendMethod(MethodDeclaration method) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < method.getParameterNames().size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(method.getParameterTypes().get(i)).append(' ')
                  .append(method.getParameterNames().get(i));
            }
            appendSyntax("public " + method.getReturnType() + " " + method.getName() + "(" + sb.toString() +
                         ") {");
            pushTab();
            method.appendMethodBody(this);
            popTab();
            appendSyntax("}");
        }

        public void newline() {
            appendSyntax("");
        }

        @Override
        public String getPropertyMemberName(String propertyName) {
            return "property_" + filterName(propertyName);
        }

        @Override
        public String getMemberName(String propertyName, Object owner) {
            Map<String, String> forKey = auxMembers.get(owner);
            if (forKey == null) {
                forKey = new HashMap<>();
                auxMembers.put(owner, forKey);
            }

            String member = forKey.get(propertyName);
            if (member == null) {
                // make a new member variable name
                member = "aux_" + (memberCounter++) + "_" + filterName(propertyName);
                forKey.put(propertyName, member);
            }
            return member;
        }

        private String filterName(String name) {
            return name.replaceAll("[^a-zA-Z0-9_]", "");
        }

        @Override
        public String getComponentIndex() {
            return "index";
        }

        @Override
        public void appendSyntax(String... blobLines) {
            for (String line : blobLines) {
                for (int i = 0; i < tabCount; i++) {
                    source.append('\t');
                }
                source.append(line).append('\n');
            }
        }
    }
}
