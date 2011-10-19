package com.entreri.property;

import com.entreri.EntitySystem;

/**
 * CompactAwareProperty is an extension of Property that receives notification
 * from the EntitySystem when it has completed a compaction (i.e. a call to
 * {@link EntitySystem#compact()}). This is most useful for properties which
 * cache values based on the last used index, which is invalidated when the
 * system is compacted.
 * 
 * @author Michael Ludwig
 */
public interface CompactAwareProperty extends Property {
    public void onCompactComplete();
}
