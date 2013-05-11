package com.lhkbob.entreri;

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
 *
 */
abstract class ComponentFactoryProvider {
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final String ABSTRACT_COMPONENT_NAME = AbstractComponent.class
            .getSimpleName();
    private static final String COMPONENT_REPO_NAME = ComponentRepository.class
            .getSimpleName();
    private static final String PROXY_PACKAGE_NAME = ComponentFactoryProvider.class
            .getPackage().getName();

    private static final ComponentFactoryProvider INSTANCE = new CachingDelegatingFactoryProvider();

    public static interface Factory<T extends Component> {
        public AbstractComponent<T> newInstance(ComponentRepository<T> forRepository);

        public List<PropertySpecification> getSpecification();
    }

    public abstract <T extends Component> Factory<T> getFactory(Class<T> componentType);

    public static ComponentFactoryProvider getInstance() {
        return INSTANCE;
    }

    private static String safeName(Class<?> type) {
        return type.getName().replace('$', '.');
    }

    protected static String getImplementationClassName(Class<? extends Component> type,
                                                       boolean includePackage) {
        // first get the simple name, concatenating all types in the hierarchy
        // (e.g. if its an inner class the name is Foo$Blah and this converts it to FooBlah)
        String scrubbed = type.getSimpleName().replace("[\\.$]", "");

        // compute a unique hash on the original canonical class name to guarantee
        // the uniqueness of the generated class as well
        long hash;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(type.getCanonicalName().getBytes(CHARSET));

            ByteBuffer md5 = ByteBuffer.wrap(md.digest());
            hash = Math.abs(md5.getLong() ^ md5.getLong());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("JVM does not support MD5", e);
        }

        StringBuilder sb = new StringBuilder();
        if (includePackage) {
            sb.append(PROXY_PACKAGE_NAME).append('.');
        }
        sb.append(scrubbed).append("Impl").append(hash);

        return sb.toString();
    }

    protected static String generateJavaCode(Class<? extends Component> type,
                                             List<PropertySpecification> spec) {
        String baseTypeName = safeName(type);

        String implName = getImplementationClassName(type, false);

        // the implementation will extend AbstractComponent sans generics because
        // Janino does not support them right now
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(PROXY_PACKAGE_NAME).append(";\n")
          .append("public class ").append(implName).append(" extends ")
          .append(ABSTRACT_COMPONENT_NAME).append(" implements ").append(baseTypeName)
          .append("{\n");

        // add property instances with proper cast so we don't have to do that every
        // time a property is accessed, and add any shared instance field declarations
        int property = 0;
        for (PropertySpecification s : spec) {
            sb.append("\tprivate final ").append(safeName(s.getPropertyType()))
              .append(" property").append(property).append(";\n");
            if (s.isSharedInstance()) {
                sb.append("\tprivate final ").append(safeName(s.getType()))
                  .append(" sharedInstance").append(property).append(";\n");
            }
            property++;
        }

        // define the constructor, must invoke super, assign properties, and allocate
        // shared instances; as with type declaration we cannot use generics
        // FIXME make owner a magic constant and sharedInstance a magic constant
        sb.append("\n\tpublic ").append(implName).append("(").append(COMPONENT_REPO_NAME)
          .append(" owner) {\n").append("\t\tsuper(owner);\n");
        property = 0;
        for (PropertySpecification s : spec) {
            sb.append("\t\tproperty").append(property).append(" = (")
              .append(safeName(s.getPropertyType())).append(") owner.getProperty(")
              .append(property).append(");\n");
            if (s.isSharedInstance()) {
                // FIXME need to add  a createShareableInstance(),
                // FIXME unless I want to create the objects via reflection ...
                sb.append("\t\tsharedInstance").append(property).append(" = ")
                  .append("property").append(property)
                  .append(".createShareableInstance();\n");
            }
            property++;
        }
        sb.append("\t}\n");

        // implement all methods of the interface
        for (Method m : type.getMethods()) {
            // FIXME ugly
            if (m.getDeclaringClass().equals(Component.class) ||
                m.getDeclaringClass().equals(Owner.class) ||
                m.getDeclaringClass().equals(Ownable.class) ||
                m.getDeclaringClass().equals(Object.class) ||
                m.getDeclaringClass().equals(AbstractComponent.class)) {
                continue;
            }
            if (!appendGetter(m, spec, sb)) {
                if (!appendSetter(m, spec, sb)) {
                    throw new IllegalStateException(
                            "Unexpected method during code generation: " + m);
                }
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    private static boolean appendGetter(Method getter, List<PropertySpecification> spec,
                                        StringBuilder sb) {
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
            sb.append("property").append(idx).append(".get(getIndex(), ")
              .append("sharedInstance").append(idx)
              .append(");\n\t\treturn sharedInstance").append(idx).append(";");
        } else {
            sb.append("return property").append(idx).append(".get(getIndex());");
        }

        sb.append("\n\t}\n");
        return true;
    }

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
            sb.append(safeName(p.getType())).append(" prop").append(spec.indexOf(p));
        }
        sb.append(") {\n");

        // implement the body
        for (PropertySpecification p : params) {
            int idx = spec.indexOf(p);
            sb.append("\t\tproperty").append(idx).append(".set(prop").append(idx)
              .append(", getIndex());\n");
        }

        // return this component if we're not a void setter
        if (returnComponent) {
            sb.append("\t\treturn this;\n");
        }
        sb.append("\t}\n");
        return true;
    }

    private static PropertySpecification findPropertyForGetter(Method getter,
                                                               List<PropertySpecification> spec) {
        for (PropertySpecification s : spec) {
            if (s.getGetterMethod().equals(getter)) {
                return s;
            }
        }
        return null;
    }

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
