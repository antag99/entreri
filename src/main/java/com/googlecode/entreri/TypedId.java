/*
 * Entreri, an entity-component framework in Java
 *
 * Copyright (c) 2011, Michael Ludwig (lhkbob@gmail.com)
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


/**
 * <p>
 * TypedId is a dynamically assigned identifier unique to the desired class
 * types. Every instance of a type T will use the same TypedId. TypedId is a
 * glorified integer id assigned to set of class types that share a common
 * parent class. Each class will be assigned a unique id within the currently
 * executing JVM.
 * </p>
 * <p>
 * The parent class of a set of TypedIds is responsible for exposing a method to
 * create TypedIds and ensuring the uniqueness of the integer ids assigned. In
 * general this should be a static method, with
 * {@link Component#getTypedId(Class)} as an example.
 * </p>
 * <p>
 * TypedId's enable typed, random-access lookups.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The identified type
 */
public class TypedId<T> {
    private final Class<T> type;
    
    private final int id;

    /**
     * Create a new Id for the given type, with the given numeric id. Subclasses
     * are responsible for guaranteeing the uniqueness of <tt>id</tt>. This
     * constructor is protected so that TypedIds cannot be created directly,
     * instead another class or system should manage their id's and uniqueness.
     * 
     * @param type The type that is identified
     * @param id The unique numeric id
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if id is less than 0
     */
    protected TypedId(Class<T> type, int id) {
        // Sanity checks, shouldn't happen if constructed by Component.getTypedId()
        if (type == null)
            throw new NullPointerException("Type cannot be null");
        if (id < 0)
            throw new IllegalArgumentException("Id must be at least 0, not: " + id);

        this.type = type;
        this.id = id;
    }

    /**
     * Return the type that this TypedId corresponds to. All instances of the
     * returned type will have the same TypedId.
     * 
     * @return The type that corresponds to this id
     */
    public Class<T> getType() {
        return type;
    }
    
    /**
     * Return the numeric id corresponding to this ComponentId. This id is
     * unique such that a ComponentId corresponding to a different
     * {@link Component} implementation will not have the same id.
     * 
     * @return The numeric id, which will be at least 0
     */
    public int getId() {
        return id;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TypedId))
            return false;
        TypedId<?> cid = (TypedId<?>) o;
        return cid.id == id && cid.type.equals(type);
    }

    @Override
    public int hashCode() {
        return id;
    }
    
    @Override
    public String toString() {
        return "TypedId (" + type.getSimpleName() + ", id=" + id + ")";
    }
}
