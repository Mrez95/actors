package com.qklabs;

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
}
