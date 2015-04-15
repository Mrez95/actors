package com.qklabs.actor;

import java.lang.ref.WeakReference;

class ActorRefImpl implements ActorRef {

    private final ActorSystem mSystem;
    private final WeakReference<Actor> mActor;
    private final String mPath;

    public ActorRefImpl(ActorSystem system, Actor actor, String path) {
        mSystem = system;
        mActor = new WeakReference<>(actor);
        mPath = path;
    }

    @Override
    public void tell(Object message, ActorRef sender) {
        mSystem.send(mActor.get(), message, sender);
    }

    @Override
    public void tell(Object message) {
        tell(message, mSystem.getEmptyActorRef());
    }

    Actor getActor() {
        return mActor.get();
    }

    @Override
    public String getPath() {
        return mPath;
    }
}
