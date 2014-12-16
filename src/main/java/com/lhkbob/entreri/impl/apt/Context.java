package com.lhkbob.entreri.impl.apt;

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
 * Context
 * =======
 *
 * Context bundles up the commonly needed information when processing a specific component type. This
 * includes the actual component type, the APT processing environment, the attribute annotation scope that all
 * method patterns are interested in, and the type mappers for both value and reference semantics. It also
 * provides utility methods for dealing with complexities of the Java mirror API.
 *
 * @author Michael Ludwig
 */
public class Context {
    private final ProcessingEnvironment env;
    private final TypeMirror componentType;
    private final TypePropertyMapper valueMapper;
    private final TypePropertyMapper referenceMapper;
    private final Set<Class<? extends Annotation>> attrScope;

    /**
     * Create a new Context that uses the provided ProcessingEnvironment, is for the given component type (which
     * is assumed to be a sub-interface of Component), and is configured to use the given reference
     * and value type mappers.
     *
     * @param env             The processing environment of the APT compilation unit processing the component type
     * @param componentType   The component sub-interface being processed
     * @param valueMapper     The property type mapper used for properties with value semantics
     * @param referenceMapper The property type mapper used for properties with reference semantics
     * @param attrScope       The set of attribute annotation types of interest to all method patterns
     */
    public Context(ProcessingEnvironment env, TypeMirror componentType, TypePropertyMapper valueMapper,
                   TypePropertyMapper referenceMapper, Set<Class<? extends Annotation>> attrScope) {
        this.env = env;
        this.componentType = componentType;
        this.valueMapper = valueMapper;
        this.referenceMapper = referenceMapper;
        this.attrScope = Collections.unmodifiableSet(new HashSet<>(attrScope));
    }

    /**
     * @return The unmodifiable set of attribute annotation types of interest to method patterns
     */
    public Set<Class<? extends Annotation>> getAttributeScope() {
        return attrScope;
    }

    /**
     * @return The Component sub-interface being processed
     */
    public TypeMirror getComponentType() {
        return componentType;
    }

    /**
     * @return The Elements utilities from the processing environment
     */
    public Elements getElements() {
        return env.getElementUtils();
    }

    /**
     * @return The Types utilities from the processing environment
     */
    public Types getTypes() {
        return env.getTypeUtils();
    }

    /**
     * @return The Messager logger from the processing environment
     */
    public Messager getLogger() {
        return env.getMessager();
    }

    /**
     * @return The ProcessingEnvironment for the compilation unit processing this component type
     */
    public ProcessingEnvironment getProcessingEnvironment() {
        return env;
    }

    /**
     * @return The type mapper for properties with value semantics
     */
    public TypePropertyMapper getValueTypeMapper() {
        return valueMapper;
    }

    /**
     * @return The type mapper for properties with reference semantics
     */
    public TypePropertyMapper getReferenceTypeMapper() {
        return referenceMapper;
    }

    /**
     * Safely convert the given TypeMirror to a TypeElement. This assumes the TypeMirror is one of the kinds
     * that represent a class, interface, array, or primitive.
     *
     * @param type The type to convert to an element
     * @return The TypeElement corresponding to the type mirror
     */
    public TypeElement asElement(TypeMirror type) {
        return findEnclosingTypeElement(getTypes().asElement(type));
    }

    /**
     * Create a TypeMirror from the given Class. The returned mirror will have type variables if the class
     * declared any. It is not the raw type.
     *
     * @param type The class type to convert
     * @return The TypeMirror corresponding to the class
     */
    public TypeMirror fromClass(Class<?> type) {
        return getElements().getTypeElement(type.getCanonicalName()).asType();
    }

    /**
     * Complete the parameterized method, as if it were being invoked on the given declared type,
     * `declarer`. This does nothing if the method or type do not define any type parameters. However, if the
     * class declaring the method uses type parameters, the executable element only reports the type variable
     * as its return or argument types. It must be completed with the actual chosen parameters to get the true
     * return or argument types.
     *
     * @param declarer The completed type, with all type variables filled in (e.g. `Set<String>`)
     * @param method   The method that should have its type variables updated based on `declarer`
     * @return The completed method
     */
    public ExecutableType getParameterizedMethod(TypeMirror declarer, ExecutableElement method) {
        return (ExecutableType) getTypes().asMemberOf((DeclaredType) declarer, method);
    }

    /**
     * Get all methods declared in `type` that match the pattern. See {@link
     * #getMatchingMethods(javax.lang.model.type.TypeMirror, java.util.List, java.util.regex.Pattern,
     * javax.lang.model.type.TypeMirror, javax.lang.model.type.TypeMirror...)} for details on how the methods
     * are matched.
     *
     * @param type        The type whose methods are matched against
     * @param namePattern The regex that the method names must match
     * @param returnType  The return type the method must have, or null if the return type can be anything
     * @param params      The parameter types the method must have, or null if the method can have any
     *                    parameters; null elements within the array represent a wildcard
     *                    type for that particular parameter
     * @return All matched elements, in the order they were reported from the type
     */
    public List<ExecutableElement> getMatchingMethods(TypeMirror type, Pattern namePattern,
                                                      TypeMirror returnType, TypeMirror... params) {
        List<? extends ExecutableElement> methods = ElementFilter.methodsIn(getElements()
                                                                                    .getAllMembers(asElement(type)));
        return getMatchingMethods(type, methods, namePattern, returnType, params);
    }

    /**
     * Get all methods from `methods` that match the given pattern. It is assumed that these methods are a
     * sublist of the methods declared in `type`. Methods are first matched against the name regular
     * expression, `namePattern`. If they pass, then their return type is compared to `returnType`. If
     * `returnType` is null, the return type of the method can be anything; otherwise it must identical. If
     * the return type matches, the method parameters are compared to `params`.
     *
     * When `params` is null, the method may have any number of parameters of any type. If it is not null,
     * the method must have the same number of parameters as the length of `params`. When `params` is not
     * null, each parameter of the method must match the corresponding element of `params`. If the element is
     * null, the method parameter can be of any type; if it is not null the types must be identical.
     *
     * This automatically completes any parameterization of the methods based on the type variable
     * assignment within `type`.
     *
     * @param type        The type whose methods are matched against
     * @param namePattern The regex that the method names must match
     * @param returnType  The return type the method must have, or null if the return type can be anything
     * @param params      The parameter types the method must have, or null if the method can have any
     *                    parameters; null elements within the array represent a wildcard
     *                    type for that particular parameter
     * @return All matched elements, in the order they were reported from the type
     */
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

    /**
     * A convenience method to determine if the type declares the exact method specified. This relies on
     * `getMatchingMethods` under the hood, so the handling of null values for `returnType` and `params`
     * is identical. The method name pattern is created so that it must identically match `methodName`.
     *
     * @param targetType The type whose methods are searched through
     * @param methodName The method name that must be found
     * @param returnType The return type the method must have, or null if the return type can be anything
     * @param params     The parameter types the method must have, or null if the method can have any
     *                   parameters; null elements within the array represent a wildcard
     *                   type for that particular parameter
     */
    public boolean hasMethod(TypeMirror targetType, String methodName, TypeMirror returnType,
                             TypeMirror... params) {
        return !getMatchingMethods(targetType, Pattern.compile(methodName), returnType, params).isEmpty();
    }

    /**
     * Utility method to determine the TypeElement that encloses the given element, regardless of the exact
     * type of element `e` is.
     *
     * @param e The element in question
     * @return The TypeElement enclosing `e`.
     */
    public static TypeElement findEnclosingTypeElement(Element e) {
        while (e != null && !(e instanceof TypeElement)) {
            e = e.getEnclosingElement();
        }
        return TypeElement.class.cast(e);
    }
}
