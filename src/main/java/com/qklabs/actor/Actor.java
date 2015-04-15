package com.qklabs.actor;

public class Actor {
    private ActorRef mSelf;

    public void preStart() {
    }

    public void onReceive(Object message, ActorRef sender) {
    }

    public void postStop() {
    }

    public ActorRef self() {
        return mSelf;
    }

    public String getPath() {
        return mSelf.getPath();
    }
}