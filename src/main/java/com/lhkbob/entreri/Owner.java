package com.lhkbob.entreri;


public interface Owner {
    public void notifyOwnershipGranted(Ownable obj);

    public void notifyOwnershipRevoked(Ownable obj);
}
