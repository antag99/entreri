package com.lhkbob.entreri.impl;

import com.lhkbob.entreri.Component;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.List;

/**
 *
 */
public interface ComponentSpecification {
    public String getType();

    public String getPackage();

    public List<? extends PropertyDeclaration> getProperties();

    public static final class Factory {
        private Factory() {
        }

        public static ComponentSpecification fromClass(Class<? extends Component> type) {
            return new ReflectionComponentSpecification(type);
        }

        public static ComponentSpecification fromTypeElement(TypeElement type,
                                                             ProcessingEnvironment env) {
            return new MirrorComponentSpecification(type, env.getTypeUtils(),
                                                    env.getElementUtils(),
                                                    env.getFiler());
        }
    }
}
