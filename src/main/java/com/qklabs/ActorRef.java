package com.qklabs;

public interface ActorRef {
    public void tell(Object message, ActorRef sender);
}
