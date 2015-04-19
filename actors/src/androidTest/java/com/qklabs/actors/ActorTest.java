package com.qklabs.actors;

import junit.framework.TestCase;

public class ActorTest extends TestCase {

    private ActorSystem system;

    @Override
    public void setUp() {
        system = new ActorSystem();
    }

    public void testSelf() {
        ActorRef ref = system.getOrCreateActor("/actor", Actor.class);
        Actor actor = ((ActorRefImpl)ref).getActor();
        assertNotNull("actor should have reference to itself", actor.self());
    }

    public void testGetPath() {
        ActorRef ref = system.getOrCreateActor("/actor", Actor.class);
        Actor actor = ((ActorRefImpl)ref).getActor();
        assertEquals("actor should have its own path", "/actor", actor.getPath());
    }
}