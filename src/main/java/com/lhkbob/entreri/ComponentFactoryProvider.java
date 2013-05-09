package com.lhkbob.entreri;

import java.util.List;
import java.util.Set;

/**
 *
 */
abstract class ComponentFactoryProvider {
    public static interface Factory<T extends Component> {
        public AbstractComponent<T> newInstance(ComponentRepository<T> forRepository);

        public List<PropertySpecification> getSpecification();

        public Set<Class<? extends Component>> getRequiredTypes();
    }

    public abstract <T extends Component> Factory<T> getFactory(Class<T> componentType);

    public static ComponentFactoryProvider getInstance() {
        // FIXME impl
        throw new UnsupportedOperationException();
    }

    public static String generateJavaCode(Class<? extends Component> type,
                                          List<PropertySpecification> spec) {
        String implName = type.getSimpleName() + "Impl";

        // FIXME what package do we put the generated classes in?
        // FIXME if AbstractComponent remains package-private then we can't put them
        // FIXME in the package of the incoming type
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(type.getPackage().getName()).append(";\n")
          .append("public class ").append(implName)
          .append(" extends com.lhkbob.entreri.AbstractComponent implements ")
          .append(type.getName()).append("{\n");
        // FIXME can't just use class.getName(), have to replace the $ with a .

        // FIXME handle shared instances needing members, and multi-param setters
        int i = 0;
        for (PropertySpecification s : spec) {
            // FIXME nicer access to this
            Class<?> pt = s.getGetterMethod().getReturnType();
            // FIXME handle return this component setters as well
            sb.append("\tpublic void ").append(s.getSetterMethod().getName()).append('(')
              .append(pt.getName()).append(' ').append(s.getName()).append(") {\n")
              .append("\t\towner.getProperty(").append(i).append(").set(")
              .append(s.getName()).append(", getIndex());\n\t}\n\n");
            sb.append("\tpublic ").append(pt.getName()).append(' ')
              .append(s.getGetterMethod().getName()).append("() {\n")
              .append("\t\treturn owner.getProperty(").append(i)
              .append(").get(getIndex());\n\t}\n\n");
            i++;
        }
        sb.append("}\n");
        return sb.toString();
    }
}
