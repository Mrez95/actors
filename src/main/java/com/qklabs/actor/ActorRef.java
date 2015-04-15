package com.qklabs.actor;

public interface ActorRef {
    public void tell(Object message, ActorRef sender);
    public void tell(Object message);
    public String getPath();
}
