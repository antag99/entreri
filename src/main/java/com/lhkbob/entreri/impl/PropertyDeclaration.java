package com.lhkbob.entreri.impl;

import com.lhkbob.entreri.property.PropertyFactory;

/**
 *
 */
public interface PropertyDeclaration extends Comparable<PropertyDeclaration> {
    public String getName();

    public String getType();

    public String getPropertyImplementation();

    public String getSetterMethod();

    public String getGetterMethod();

    public int getSetterParameter();

    public boolean getSetterReturnsComponent();

    public boolean isShared();

    public PropertyFactory<?> getPropertyFactory();
}
