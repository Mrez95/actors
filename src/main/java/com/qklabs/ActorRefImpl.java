package com.qklabs;

class ActorRefImpl implements ActorRef {

    private final ActorSystem mSystem;
    private final Actor mActor;

    public ActorRefImpl(ActorSystem system, Actor actor) {
        mSystem = system;
        mActor = actor;
    }

    @Override
    public void tell(Object message, ActorRef sender) {
        mSystem.send(mActor, message, sender);
    }

    Actor getActor() {
        return mActor;
    }
}
