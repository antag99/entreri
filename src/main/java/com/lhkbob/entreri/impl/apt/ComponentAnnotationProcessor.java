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
 * ComponentImplementationProcessor
 * ================================
 *
 * ComponentImplementationProcessor is an annotation processor to use with Java 6+ compilers or APT to generate
 * component proxy implementations for all component sub-interfaces encountered in the build class path. These
 * will then be dynamically loaded at runtime instead of using something such as Janino to generate classes
 * from scratch.
 *
 * @author Michael Ludwig
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ComponentAnnotationProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Types tutil = processingEnv.getTypeUtils();
        Elements eutil = processingEnv.getElementUtils();
        for (Element e : roundEnv.getRootElements()) {
            if (e.getKind().equals(ElementKind.INTERFACE)) {
                // we have an interface
                TypeElement t = (TypeElement) e;
                if (tutil.isAssignable(t.asType(),
                                       eutil.getTypeElement(Component.class.getCanonicalName()).asType())) {

                    try {
                        ComponentSpecification spec = new ComponentSpecification(t, processingEnv);
                        String name = ComponentGenerator.getImplementationClassName(spec, true);
                        String code = new ComponentGenerator().generateJavaCode(spec);

                        try {
                            Writer w = processingEnv.getFiler().createSourceFile(name, t).openWriter();
                            w.write(code);
                            w.flush();
                            w.close();
                        } catch (IOException ioe) {
                            processingEnv.getMessager()
                                         .printMessage(Diagnostic.Kind.ERROR, ioe.getMessage(), t);
                        }
                    } catch (IllegalComponentDefinitionException ec) {
                        String typePrefix = t.asType().toString();
                        String msg = ec.getMessage();
                        if (msg.startsWith(typePrefix)) {
                            msg = msg.substring(typePrefix.length());
                        }
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, t);
                    }
                }
            }
        }

        return false;
    }
}
