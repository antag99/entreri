package com.lhkbob.entreri;

import com.sun.tools.javac.resources.compiler;
import org.codehaus.janino.ClassBodyEvaluator;
import org.codehaus.janino.SimpleCompiler;

/**
 *
 */
public class JaninoTest {
    public static API createInstanceNativeJanino() throws Exception {
//        ClassBodyEvaluator compiler = new ClassBodyEvaluator();
        SimpleCompiler compiler = new SimpleCompiler();
        compiler.setParentClassLoader(ClassLoader.getSystemClassLoader());
        compiler.cook(CODE_TEXT);

        Class<?> cls = compiler.getClassLoader().loadClass("com.lhkbob.entreri.JaninoImpl");
        return (API) cls.newInstance();
    }

    public static API createInstanceJDKJanino() throws Exception {
        org.codehaus.commons.compiler.jdk.SimpleCompiler compiler = new org.codehaus.commons.compiler.jdk.SimpleCompiler();
        compiler.setParentClassLoader(ClassLoader.getSystemClassLoader());
        compiler.cook(CODE_TEXT);

        Class<?> cls = compiler.getClassLoader().loadClass("com.lhkbob.entreri.JaninoImpl");
        return (API) cls.newInstance();
    }

    public static void main(String[] args) throws Exception {
        APIClone clone = new APIClone();
        ExplicitImpl impl = new ExplicitImpl();

        API api1 = createInstanceNativeJanino();
        API api2 = createInstanceJDKJanino();

        System.out.println(clone.getClass().getCanonicalName());
        System.out.println(impl.getClass().getCanonicalName());
        System.out.println(api1.getClass().getCanonicalName());
        System.out.println(api2.getClass().getCanonicalName());

        System.out.println(api1 instanceof API);
        System.out.println(api2 instanceof API);
        System.out.println(api1.getClass().equals(api2.getClass()));

        int numTests = 1000;
        int numIters = 100000;
        {
            int sum = 0;
            long now = System.nanoTime();
            for (int i = 0; i < numTests; i++) {
                sum += testClone(clone, numIters);
            }
            System.out.println("CLONE: " + (System.nanoTime() - now) + " " + sum);
        }

        {
            int sum = 0;
            long now = System.nanoTime();
            for (int i = 0; i < numTests; i++) {
                sum += testAPI(api1, numIters);
            }
            System.out.println("JANINO: " + (System.nanoTime() - now) + " " + sum);
        }

        {
            int sum = 0;
            long now = System.nanoTime();
            for (int i = 0; i < numTests; i++) {
                sum += testAPI(api2, numIters);
            }
            System.out.println("JDK: " + (System.nanoTime() - now) + " " + sum);
        }

        {
            int sum = 0;
            long now = System.nanoTime();
            for (int i = 0; i < numTests; i++) {
                sum += testAPI(impl, numIters);
            }
            System.out.println("IMPL: " + (System.nanoTime() - now) + " " + sum);
        }

        {
            int sum = 0;
            long now = System.nanoTime();
            for (int i = 0; i < numTests; i++) {
                sum += testClone(clone, numIters);
            }
            System.out.println("CLONE: " + (System.nanoTime() - now) + " " + sum);
        }

        {
            int sum = 0;
            long now = System.nanoTime();
            for (int i = 0; i < numTests; i++) {
                sum += testAPI(api1, numIters);
            }
            System.out.println("JANINO: " + (System.nanoTime() - now) + " " + sum);
        }

        {
            int sum = 0;
            long now = System.nanoTime();
            for (int i = 0; i < numTests; i++) {
                sum += testAPI(api2, numIters);
            }
            System.out.println("JDK: " + (System.nanoTime() - now) + " " + sum);
        }

        {
            int sum = 0;
            long now = System.nanoTime();
            for (int i = 0; i < numTests; i++) {
                sum += testAPI(impl, numIters);
            }
            System.out.println("IMPL: " + (System.nanoTime() - now) + " " + sum);
        }
    }

    public static int testClone(APIClone instance, int numIters) {
        int sum = 0;
        for (int i = 0; i < numIters; i++) {
            instance.setValue(i);
            sum += instance.getValue();
        }
        return sum;
    }

    public static int testAPI(API instance, int numIters) {
        int sum = 0;
        for (int i = 0; i < numIters; i++) {
            instance.setValue(i);
            sum += instance.getValue();
        }
        return sum;
    }

    public static class APIClone {
        private int value;

        public int getValue() {
            return value;
        }

        public void setValue(int v) {
            value = v;
        }
    }

    public static interface API {
        public int getValue();

        public void setValue(int v);
    }

    public static class ExplicitImpl implements API {
        private int value;

        public int getValue() {
            return value;
        }

        public void setValue(int v) {
            value = v;
        }
    }

    public static final String CODE_TEXT = "package com.lhkbob.entreri;\n" +
                                           "public class JaninoImpl implements JaninoTest.API {\n" +
                                           "        private int value;\n" +
                                           "\n" +
                                           "        public int getValue() {\n" +
                                           "            return value;\n" +
                                           "        }\n" +
                                           "\n" +
                                           "        public void setValue(int v) {\n" +
                                           "            value = v;\n" +
                                           "        }\n" +
                                           "    }";
}
