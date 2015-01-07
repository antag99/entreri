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
import com.lhkbob.entreri.impl.AbstractComponent;
import com.lhkbob.entreri.impl.ComponentDataStore;
import com.lhkbob.entreri.impl.EntitySystemImpl;
import com.lhkbob.entreri.property.Property;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ComponentGenerator
 * ==================
 *
 * ComponentGenerator is the primary implementation of {@link Generator}. It handles the class definition,
 * interfaces, and formatting of the component proxy while delegating to the properties and method
 * declarations when appropriate to append syntax implementing all required methods. The component classes it
 * defines extend {@link com.lhkbob.entreri.impl.AbstractComponent}. They all declare a static `create` method
 * that takes a single {@link com.lhkbob.entreri.impl.EntitySystemImpl} as an argument and  return a properly
 * configured ComponentDataStore that can manage instances of the generated component type.
 *
 * @author Michael Ludwig
 */
public class ComponentGenerator implements Generator {
    private static final Charset CHARSET = Charset.forName("UTF-8");

    private final ComponentSpecification spec;

    private int tabCount;
    private final StringBuilder source;

    private final Map<Object, Map<String, String>> auxMembers;
    private int memberCounter;

    /**
     * Create a new ComponentGenerator.
     */
    private ComponentGenerator(ComponentSpecification spec) {
        this.spec = spec;
        source = new StringBuilder();
        tabCount = 0;
        memberCounter = 0;
        auxMembers = new HashMap<>();
    }

    /**
     * Get the unique implementation name for the provided Component interface. If `includePackage` is true,
     * the returned string will include the package name to create a valid, absolute type name. The
     * returned string may then be passed to {@link Class#forName(String)}.
     *
     * @param cls            The component interface type
     * @param includePackage True if the package should be included
     * @return The class name that corresponds to the generated proxy implementation for the given type
     */
    public static String getImplementationClassName(Class<? extends Component> cls, boolean includePackage) {
        return getImplementationClassName(cls.getCanonicalName(), cls.getPackage().getName(), includePackage);
    }

    /**
     * Get the unique implementation name that should be used when generating source files or looking for an
     * existing proxy class that implements the given component type. If `includePackage` is true,
     * the returned string will include the package name to create a valid, absolute type name.
     *
     * @param spec           The component specification
     * @param includePackage True if the package should be included
     * @return The class name that corresponds to the generated proxy implementation for the given type
     */
    public static String getImplementationClassName(ComponentSpecification spec, boolean includePackage) {
        return getImplementationClassName(spec.getType().toString(), spec.getPackage(), includePackage);
    }

    private static String getImplementationClassName(String typeName, String packageName,
                                                     boolean includePackage) {
        // first remove the package name from typeName if necessary
        if (!packageName.isEmpty() && typeName.startsWith(packageName)) {
            typeName = typeName.substring(packageName.length() + 1);
        }

        // next get the simple name, concatenating all types in the hierarchy
        // (e.g. if its an inner class the name is Foo.Blah and this converts it to FooBlah)
        String scrubbed = typeName.replace("[\\.]", "");

        // compute a unique hash on the original canonical class name to guarantee
        // the uniqueness of the generated class as well
        int hash;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update((packageName + "." + typeName).getBytes(CHARSET));

            ByteBuffer md5 = ByteBuffer.wrap(md.digest());
            hash = Math.abs(md5.getInt(0) ^ md5.getInt(1) ^ md5.getInt(2) ^ md5.getInt(3));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("JVM does not support MD5", e);
        }

        StringBuilder sb = new StringBuilder();
        if (includePackage && !packageName.isEmpty()) {
            sb.append(packageName).append('.');
        }
        sb.append(scrubbed).append("Impl").append(hash);

        return sb.toString();
    }

    /**
     * Generate valid Java source code for a proxy implementation of the given component type. The name and
     * package of the generated class are consistent with the results of calling {@link
     * #getImplementationClassName(ComponentSpecification, boolean)}. It is assumed (and not validated) that
     * the property specification is valid and corresponds to the component type. The new class will also
     * extend from AbstractComponent and has a single constructor that takes a ComponentDataStore. It will
     * also define a static public create() method to produce an appropriate ComponentDataStore object.
     *
     * The source code that is generated requires Java 1.7 or higher to compile.
     *
     * @param spec The component specification that must be implemented
     * @return Source code of a valid implementation for the component type
     */
    public static String generateJavaCode(ComponentSpecification spec) {
        return new ComponentGenerator(spec).generate();
    }


    private String generate() {
        source.setLength(0);

        List<? extends MethodDeclaration> methods = spec.getMethods();

        // begin outputting source code
        String implName = getImplementationClassName(spec, false);
        String genericParam = "<" + spec.getType() + ">";

        if (!spec.getPackage().isEmpty()) {
            appendSyntax("package " + spec.getPackage() + ";");
            newline();
        }

        // prepend some annotations (and get UTC formatted date, as required by
        // the @Generated annotation if we attach a date, which we do because it's useful)
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());

        appendSyntax("import javax.annotation.Generated;", "",
                     "@Generated(value={\"" + ComponentGenerator.class.getName() +
                     "\"}, date=\"" + nowAsISO + "\")", "@SuppressWarnings(\"unchecked\")");

        appendSyntax("public final class " + implName + " extends " + AbstractComponent.class.getName() +
                     genericParam + " implements " + spec.getType() + " {");

        appendSyntax("private final DataStoreImpl data;");
        newline();

        // note that the property fields are stored in a ComponentDataStore subclass not the Component subclass
        for (MethodDeclaration m : methods) {
            m.appendMembers(this);
        }
        newline();

        // define the constructor, must invoke super, assign properties, and allocate
        // shared instances; as with type declaration we cannot use generics
        appendSyntax("private " + implName + "(DataStoreImpl repo) {");
        appendSyntax("super(repo);", "data = repo;");

        appendSyntax("// any extra member initialization");
        for (MethodDeclaration m : methods) {
            m.appendConstructorInitialization(this);
        }
        appendSyntax("}");
        newline();

        // now add all methods
        for (MethodDeclaration m : methods) {
            appendMethod(m);
            newline();
        }

        // add static creation method for the component data store
        appendSyntax("public static " + ComponentDataStore.class.getName() + genericParam + " create(" +
                     EntitySystemImpl.class.getName() + " system) {",
                     Map.class.getName() + "<" + String.class.getName() + ", " + Property.class.getName() +
                     "> properties = new " + HashMap.class.getName() + "<>();");
        appendSyntax("try {");
        for (PropertyDeclaration p : spec.getProperties()) {
            appendSyntax("properties.put(\"" + p.getName() + "\", " + p.getConstructorInvocationSyntax() +
                         ");");
        }
        appendSyntax("} catch(Exception e) {",
                     "throw new RuntimeException(\"Unable to inspect attribute annotations\", e);", "}");

        newline();
        appendSyntax("return new DataStoreImpl(system, properties);", "}");

        newline();
        newline();
        // add an internal ComponentDataStore subclass that stores each property as a field for direct access
        appendSyntax("private static class DataStoreImpl extends " + ComponentDataStore.class.getName() +
                     genericParam + " {");
        // add property instances with proper cast so we don't have to do that every
        // time a property is accessed, and add any shared instance field declarations
        for (PropertyDeclaration s : spec.getProperties()) {
            appendSyntax("private final " + s.getPropertyImplementation() + " " +
                         getPropertyMemberName(s.getName(), true) + ";");
        }
        newline();

        appendSyntax("public DataStoreImpl(" + EntitySystemImpl.class.getName() + " system, " +
                     Map.class.getName() + "<" + String.class.getName() + ", " + Property.class.getName() +
                     "> properties) {",
                     "super(system, " + spec.getType().toString() + ".class, properties);");
        for (PropertyDeclaration s : spec.getProperties()) {
            appendSyntax(getPropertyMemberName(s.getName(), true) + " = (" +
                         s.getPropertyImplementation() + ") properties.get(\"" + s.getName() + "\");");
        }
        appendSyntax("}");
        newline();

        // override the createDataInstance() method
        appendSyntax("@Override", "public " + implName + " createDataInstance() {", "return new " + implName +
                                                                                    "(this);", "}");
        // close the data store class
        appendSyntax("}");

        // close the outer component class
        appendSyntax("}");
        newline();
        return source.toString();
    }

    private void appendMethod(MethodDeclaration method) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < method.getParameterNames().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(method.getParameterTypes().get(i)).append(' ')
              .append(method.getParameterNames().get(i));
        }
        appendSyntax("@Override");
        appendSyntax("public " + method.getReturnType() + " " + method.getName() + "(" + sb.toString() +
                     ") {");
        method.appendMethodBody(this);
        appendSyntax("}");
    }

    private void newline() {
        appendSyntax("");
    }

    @Override
    public String getPropertyMemberName(String propertyName) {
        return getPropertyMemberName(propertyName, false);
    }

    private String getPropertyMemberName(String propertyName, boolean inDataStoreImpl) {
        String inDataStoreImplName = "property_" + filterName(propertyName);
        if (!inDataStoreImpl) {
            return "data." + inDataStoreImplName;
        } else {
            return inDataStoreImplName;
        }
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
        int hash = name.hashCode();
        return name.replaceAll("[^a-zA-Z0-9_]", "") + "_" + Integer.toHexString(hash);
    }

    @Override
    public String getComponentIndex() {
        return "index";
    }

    @Override
    public void appendSyntax(String... blobLines) {
        for (String line : blobLines) {
            if (line.startsWith("}")) {
                tabCount = Math.max(0, tabCount - 1);
            }
            for (int i = 0; i < tabCount; i++) {
                source.append('\t');
            }
            if (line.endsWith("{")) {
                tabCount++;
            }

            source.append(line).append('\n');
        }
    }

    @Override
    public Context getContext() {
        return spec.getContext();
    }
}
