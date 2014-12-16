package com.lhkbob.entreri.impl.apt;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.regex.Pattern;

/**
 *
 */
public class Context {
    private final ProcessingEnvironment env;
    private final TypeMirror componentType;
    private final TypePropertyMapper valueMapper;
    private final TypePropertyMapper referenceMapper;
    private final Set<Class<? extends Annotation>> attrScope;

    public Context(ProcessingEnvironment env, TypeMirror componentType, TypePropertyMapper valueMapper,
                   TypePropertyMapper referenceMapper, Set<Class<? extends Annotation>> attrScope) {
        this.env = env;
        this.componentType = componentType;
        this.valueMapper = valueMapper;
        this.referenceMapper = referenceMapper;
        this.attrScope = Collections.unmodifiableSet(new HashSet<>(attrScope));
    }

    public Set<Class<? extends Annotation>> getAttributeScope() {
        return attrScope;
    }

    public TypeMirror getComponentType() {
        return componentType;
    }

    public Elements getElements() {
        return env.getElementUtils();
    }

    public Types getTypes() {
        return env.getTypeUtils();
    }

    public Messager getLogger() {
        return env.getMessager();
    }

    public Filer getFileIO() {
        return env.getFiler();
    }

    public ProcessingEnvironment getProcessingEnvironment() {
        return env;
    }

    public TypePropertyMapper getValueTypeMapper() {
        return valueMapper;
    }

    public TypePropertyMapper getReferenceTypeMapper() {
        return referenceMapper;
    }

    public TypeElement asElement(TypeMirror type) {
        return findEnclosingTypeElement(getTypes().asElement(type));
    }

    public TypeMirror fromClass(Class<?> type) {
        return getElements().getTypeElement(type.getCanonicalName()).asType();
    }

    public ExecutableType getParameterizedMethod(TypeMirror declarer, ExecutableElement method) {
        return (ExecutableType) getTypes().asMemberOf((DeclaredType) declarer, method);
    }

    public List<ExecutableElement> getMatchingMethods(TypeMirror type, Pattern namePattern,
                                                      TypeMirror returnType, TypeMirror... params) {
        List<? extends ExecutableElement> methods = ElementFilter.methodsIn(getElements()
                                                                                    .getAllMembers(asElement(type)));
        return getMatchingMethods(type, methods, namePattern, returnType, params);
    }

    public List<ExecutableElement> getMatchingMethods(TypeMirror type,
                                                      List<? extends ExecutableElement> methods,
                                                      Pattern namePattern, TypeMirror returnType,
                                                      TypeMirror... params) {
        List<ExecutableElement> matches = new ArrayList<>();
        Types ty = getTypes();

        for (ExecutableElement m : methods) {
            if (namePattern.matcher(m.getSimpleName()).matches()) {
                ExecutableType parameterizedM = getParameterizedMethod(type, m);

                // check return type (null return type means it is flexible)
                if (returnType != null && !ty.isSameType(returnType, parameterizedM.getReturnType())) {
                    continue;
                }
                // check parameters, a null array is flexible in its entirety, a null parameter is flexible
                // for that particular parameter
                if (params != null) {
                    List<? extends TypeMirror> realParams = parameterizedM.getParameterTypes();
                    if (params.length != realParams.size()) {
                        continue;
                    }
                    boolean match = true;
                    for (int i = 0; i < params.length; i++) {
                        if (params[i] != null && !ty.isSameType(params[i], realParams.get(i))) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        matches.add(m);
                    }
                } else {
                    matches.add(m);
                }

            }
        }

        return matches;
    }

    public boolean hasMethod(TypeMirror targetType, String methodName, TypeMirror returnType,
                             TypeMirror... params) {
        return !getMatchingMethods(targetType, Pattern.compile(methodName), returnType, params).isEmpty();
    }

    public static TypeElement findEnclosingTypeElement(Element e) {
        while (e != null && !(e instanceof TypeElement)) {
            e = e.getEnclosingElement();
        }
        return TypeElement.class.cast(e);
    }
}
