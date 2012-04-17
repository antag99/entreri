package com.lhkbob.entreri;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.lhkbob.entreri.annot.Attribute;

public class Attributes {
    private final Map<Class<? extends Annotation>, Annotation> attrs;
    
    public Attributes(Field f) {
        if (f == null)
            throw new NullPointerException("Field cannot be null");
        
        attrs = new HashMap<Class<? extends Annotation>, Annotation>();
        
        for (Annotation a: f.getAnnotations()) {
            if (a.annotationType().getAnnotation(Attribute.class) != null) {
                // the attribute is an annotation
                attrs.put(a.annotationType(), a);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAttribute(Class<T> cls) {
        if (cls == null)
            throw new NullPointerException("Annotation class cannot be null");
        return (T) attrs.get(cls);
    }
    
    public boolean hasAttribute(Class<? extends Annotation> cls) {
        if (cls == null)
            throw new NullPointerException("Annotation class cannot be null");
        return attrs.containsKey(cls);
    }
    
    public Collection<Annotation> getAttributes() {
        return attrs.values();
    }
}
