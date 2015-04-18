package com.qklabs.actors;

public interface ActorRef {
    public void tell(Object message, ActorRef sender);
    public void tell(Object message);
    public String getPath();
}
