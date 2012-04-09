package com.lhkbob.entreri.component;

import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.annot.Unmanaged;

public class OnSetComponent extends ComponentData<OnSetComponent> {
    @Unmanaged
    public int onsetIndex;
    
    @Unmanaged
    public boolean onsetCalled;
    
    protected OnSetComponent() { }
    
    @Override
    protected void onSet(int index) {
        onsetIndex = index;
        onsetCalled = true;
    }
}
