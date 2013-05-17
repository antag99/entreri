package com.lhkbob.entreri.impl;

import com.lhkbob.entreri.IllegalComponentDefinitionException;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 *
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class StaticComponentGenerator extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        Types tutil = processingEnv.getTypeUtils();
        Elements eutil = processingEnv.getElementUtils();
        for (Element e : roundEnv.getRootElements()) {
            if (e.getKind().equals(ElementKind.INTERFACE)) {
                // we have an interface
                TypeElement t = (TypeElement) e;
                if (tutil.isAssignable(t.asType(), eutil.getTypeElement(
                        "com.lhkbob.entreri.Component").asType())) {

                    try {
                        ComponentSpecification spec = ComponentSpecification.Factory
                                                                            .fromTypeElement(
                                                                                    t,
                                                                                    processingEnv);
                        String name = ComponentFactoryProvider
                                .getImplementationClassName(spec, true);
                        String code = ComponentFactoryProvider
                                .generateJavaCode(spec, true);
                        try {
                            Writer w = processingEnv.getFiler().createSourceFile(name, t)
                                                    .openWriter();
                            w.write(code);
                            w.flush();
                            w.close();
                        } catch (IOException ioe) {
                            processingEnv.getMessager()
                                         .printMessage(Diagnostic.Kind.ERROR,
                                                       ioe.getMessage(), t);
                        }
                    } catch (IllegalComponentDefinitionException ec) {
                        String typePrefix = t.asType().toString();
                        String msg = ec.getMessage();
                        if (msg.startsWith(typePrefix)) {
                            msg = msg.substring(typePrefix.length());
                        }
                        processingEnv.getMessager()
                                     .printMessage(Diagnostic.Kind.ERROR, msg, t);
                    }
                }
            }
        }

        // TODO okay so this is how I can filter it down to the components that need to be
        // TODO processed.  Unfortunately, I can't load the classes because they haven't been
        // TODO defined yet until APT finishes. This means that I need to write a new generator
        // TODO that can use these classes. They have the same requisite information, and really
        // TODO I can change the PropertySpecification to be a little more flexible (e.g. take
        // TODO string names for types, etc.?) then I can use the same generator and just need
        // TODO new code to extract the spec from a TypeElement instead of a Class
        // TODO Even this should be simple with a few visitor interfaces internally that wrap
        // TODO either getting methods, types, etc from reflection or this

        return false;
    }
}
