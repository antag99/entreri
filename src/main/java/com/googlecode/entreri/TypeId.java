/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2012, Michael Ludwig
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
package com.googlecode.entreri;

import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;


/**
 * <p>
 * TypeId is a dynamically assigned identifier unique to the desired component data
 * types. Every instance of a type T will use the same TypeId. TypeId is a
 * glorified integer id assigned to set of class types that share a common
 * parent class. Each class will be assigned a unique id within the currently
 * executing JVM.
 * </p>
 * <p>
 * TypeId's enable typed, random-access lookups.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The identified type
 */
public class TypeId<T extends ComponentData<T>> {
    // Use a ConcurrentHashMap to perform reads. It is still synchronized completely to do
    // an insert to make sure a type doesn't try to use two different id values.
    private static final ConcurrentHashMap<Class<? extends ComponentData<?>>, TypeId<? extends ComponentData<?>>> typeMap 
        = new ConcurrentHashMap<Class<? extends ComponentData<?>>, TypeId<? extends ComponentData<?>>>();
    
    private static int idSeq = 0;
    
    private final Class<T> type;
    private final int id;

    /**
     * Create a new Id for the given type, with the given numeric id. It is
     * assumed that the id will remain unique.
     * 
     * @param type The type that is identified
     * @param id The unique numeric id
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if id is less than 0
     */
    private TypeId(Class<T> type, int id) {
        // Sanity checks, shouldn't happen
        if (type == null)
            throw new NullPointerException("Type cannot be null");
        if (id < 0)
            throw new IllegalArgumentException("Id must be at least 0, not: " + id);

        this.type = type;
        this.id = id;
    }

    /**
     * Return the type that this TypeId corresponds to. All instances of the
     * returned type will have the same TypeId.
     * 
     * @return The type that corresponds to this id
     */
    public Class<T> getType() {
        return type;
    }
    
    /**
     * Return the numeric id corresponding to this ComponentId. This id is
     * unique such that a ComponentId corresponding to a different
     * {@link ComponentData} implementation will not have the same id.
     * 
     * @return The numeric id, which will be at least 0
     */
    public int getId() {
        return id;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TypeId))
            return false;
        TypeId<?> cid = (TypeId<?>) o;
        return cid.id == id && cid.type.equals(type);
    }

    @Override
    public int hashCode() {
        return id;
    }
    
    @Override
    public String toString() {
        return "TypeId (" + type.getSimpleName() + ", id=" + id + ")";
    }

    /**
     * <p>
     * Return the unique TypeId instance for the given <tt>type</tt>. If a
     * TypeId hasn't yet been created a new one is instantiated with the next
     * numeric id in the internal id sequence. The new TypeId is stored for
     * later, so that subsequent calls to {@link #get(Class)} with
     * <tt>type</tt> will return the same instance. It is recommended that a
     * ComponentData declare a static final <tt>ID</tt> holding its TypeId.
     * </p>
     * <p>
     * This method does not validate the definition of the ComponentData because
     * validation depends on the potentially customized ComponentDataFactory used
     * by each EntitySystem.
     * Additionally, abstract ComponentData types cannot have a TypeId assigned to
     * them.
     * </p>
     * 
     * @param <T> The ComponentData class type
     * @param type The Class whose TypeId is fetched, which must be a subclass
     *            of ComponentData
     * @return A unique TypeId associated with the given type
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if type is not actually a subclass of
     *             ComponentData, or if it is abstract
     */
    @SuppressWarnings("unchecked")
    public static <T extends ComponentData<T>> TypeId<T> get(Class<T> type) {
        if (type == null)
            throw new NullPointerException("Type cannot be null");
        
        // Do a look up first without locking to avoid the synchronized lock and expensive
        // error checking.  If we found one, we know it passed validation the first time, otherwise
        // we'll validate it before creating a new TypeId.
        TypeId<T> id = (TypeId<T>) typeMap.get(type);
        if (id != null)
            return id; // Found an existing id
        
        if (!ComponentData.class.isAssignableFrom(type))
            throw new IllegalArgumentException("Class does not extend ComponentData: " + type);
        if (Modifier.isAbstract(type.getModifiers()))
            throw new IllegalArgumentException("Abstract classes cannot have TypeIds: " + type);
        
        synchronized(typeMap) {
            // Must create a new id, we lock completely to prevent concurrent get() on the
            // same type using two different ids.  One would get overridden and its returned TypeId
            // would be invalid.
            // - Double check, then, before creating a new id
            id = (TypeId<T>) typeMap.get(type);
            if (id != null)
                return id; // Someone else put in the type after we checked but before we locked
            
            id = new TypeId<T>(type, idSeq++);
            typeMap.put(type, id);
            return id;
        }
    }
}
