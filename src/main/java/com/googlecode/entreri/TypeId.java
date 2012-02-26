/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2011, Michael Ludwig
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

import java.util.concurrent.ConcurrentHashMap;

import com.googlecode.entreri.annot.Unmanaged;


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
// FIXME: I need an API for assigning the ComponentDataFactory to use
// Is this a per type basis? Can I override it? Or should it only be possible
// via an annotation configuration?
// - I think that is probably best since we need this to be a consistent global
//   state
// - That being said, it's probably possible to declare a customizable default type
//   that could be used instead of the reflection impl.
// - Perhaps the factory can be specified before the TypeId is loaded?

// What is the downside to allowing it to change in the future?
//  - technically different property factories and properties,
//    it wouldn't work on another system.
// - However, we could say that they can be configured to the scope of a system,
//   since we're already locking a CD to a system.
//   Then it can fail if it's already been coerced into using a different factory.
// - This makes me think that the system should be have newInstance(TypeId) instead
//   of TypeId.newInstance(system).
//   - This makes for a better design since it re-inforces the ownership
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

    // FIXME: I believe I will have to double check this for correctness
    // FIXME: this no longer verifies the correctness, so all documentation should move
    //  - most of it goes into ReflectionComponentDataFactory
    /**
     * <p>
     * Return the unique TypeId instance for the given <tt>type</tt>. If a
     * TypeId hasn't yet been created a new one is instantiated with the next
     * numeric id in the internal id sequence. The new TypeId is stored for
     * later, so that subsequent calls to {@link #getTypedId(Class)} with
     * <tt>type</tt> will return the same instance. It is recommended that a
     * ComponentData declare a static final <tt>ID</tt> holding its TypeId.
     * </p>
     * <p>
     * This method also performs runtime checks to ensure the validity of the
     * ComponentData type definition. The following rules must be upheld or an
     * {@link IllegalComponentDefinitionException} is thrown.
     * <ul>
     * <li>If the class extends from a class other than ComponentData, that class
     * must be a subclass of ComponentData and be declared abstract. Additional
     * rules might affect these parent classes.</li>
     * <li>A concrete ComponentData type must have only one constructor; it must be
     * private and with arguments: EntitySystem, int. Abstract ComponentData types
     * do not have this restriction.</li>
     * <li>Any non-static fields defined in a ComponentData (abstract or concrete)
     * must implement Property and be declared private or protected, or be
     * annotated with {@link Unmanaged} (in which case the field is ignored.</li>
     * </ul>
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
     * @throws IllegalComponentDefinitionException if the type does not follow
     *             the definition rules described above
     * @throws SecurityException if the reflection needed to create and analyze
     *             the ComponentData fails
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
