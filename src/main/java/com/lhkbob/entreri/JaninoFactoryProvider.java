package com.lhkbob.entreri;

import org.codehaus.janino.SimpleCompiler;

/**
 *
 */
class JaninoFactoryProvider {

    public static Object createInstanceNativeJanino() throws Exception {
        //        ClassBodyEvaluator compiler = new ClassBodyEvaluator();
        SimpleCompiler compiler = new SimpleCompiler();
        compiler.setParentClassLoader(ClassLoader.getSystemClassLoader());
        compiler.cook(CODE_TEXT);

        Class<?> cls = compiler.getClassLoader()
                               .loadClass("com.lhkbob.entreri.JaninoImpl");
        return cls.newInstance();
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
