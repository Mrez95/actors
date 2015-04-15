package com.qklabs.actor;

class ActorRefImpl implements ActorRef {

    private final ActorSystem mSystem;
    private final Actor mActor;
    private final String mPath;

    public ActorRefImpl(ActorSystem system, Actor actor, String path) {
        mSystem = system;
        mActor = actor;
        mPath = path;
    }

    @Override
    public void tell(Object message, ActorRef sender) {
        mSystem.send(mActor, message, sender);
    }

    @Override
    public void tell(Object message) {
        mSystem.send(mActor, message, mSystem.getEmptyActorRef());
    }

    Actor getActor() {
        return mActor;
    }

    @Override
    public String getPath() {
        return mPath;
    }
}
