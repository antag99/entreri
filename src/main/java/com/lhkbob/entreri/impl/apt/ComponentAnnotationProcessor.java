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
import com.lhkbob.entreri.property.Property;

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
 * ComponentAnnotationProcessor
 * ================================
 *
 * ComponentAnnotationProcessor is an annotation processor to use with Java 7+ compilers or APT to
 * generate component proxy implementations for all component sub-interfaces encountered in the build class
 * path. These will then be dynamically loaded at runtime instead of using something such as Janino to
 * generate classes from scratch.
 *
 * This also validates the interface contract of all Property implementations.
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
                    generateComponentImplementation(t);
                }
            } else if (e.getKind().equals(ElementKind.CLASS)) {
                // check if the class implements Property
                TypeElement t = (TypeElement) e;
                if (tutil.isAssignable(t.asType(),
                                       eutil.getTypeElement(Property.class.getCanonicalName()).asType())) {
                    validatePropertyImplementation(t);
                }
            }
        }

        return false;
    }

    private void validatePropertyImplementation(TypeElement property) {
        Types tu = processingEnv.getTypeUtils();
        Elements eu = processingEnv.getElementUtils();

        boolean referenceSemantics = tu.isAssignable(property.asType(),
                                                     eu.getTypeElement(Property.ReferenceSemantics.class
                                                                               .getCanonicalName()).asType());
        boolean valueSemantics = tu.isAssignable(property.asType(),
                                                 eu.getTypeElement(Property.ValueSemantics.class
                                                                           .getCanonicalName()).asType());
        if (!(referenceSemantics ^ valueSemantics)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                     "Must implement only one of ReferenceSemantics or ValueSemantics",
                                                     property);
        }

        if (property.getTypeParameters().size() > 0) {
            if (!tu.isAssignable(property.asType(),
                                 eu.getTypeElement(Property.Generic.class.getCanonicalName()).asType())) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                         "Property with type variables must implement Generic",
                                                         property);
            }
        }
    }

    private void generateComponentImplementation(TypeElement component) {
        try {
            // generate a custom Component implementation for this Component interface
            ComponentSpecification spec = new ComponentSpecification(component, processingEnv);
            String name = ComponentGenerator.getImplementationClassName(spec, true);
            String code = new ComponentGenerator().generateJavaCode(spec);

            try {
                Writer w = processingEnv.getFiler().createSourceFile(name, component).openWriter();
                w.write(code);
                w.flush();
                w.close();
            } catch (IOException ioe) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ioe.getMessage(), component);
            }
        } catch (IllegalComponentDefinitionException ec) {
            String typePrefix = component.asType().toString();
            String msg = ec.getMessage();
            if (msg.startsWith(typePrefix)) {
                msg = msg.substring(typePrefix.length());
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, component);
        }
    }
}
