package com.qklabs.actor;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ActorTest {

    private ActorSystem system;

    @Before
    public void setupActorSystem() {
        system = new ActorSystem();
    }

    @Test
    public void testSelf() {
        ActorRef ref = system.getOrCreateActor(Actor.class, "/actor");
        Actor actor = ((ActorRefImpl)ref).getActor();
        assertNotNull("actor should have reference to itself", actor.self());
    }

    @Test
    public void testGetPath() {
        ActorRef ref = system.getOrCreateActor(Actor.class, "/actor");
        Actor actor = ((ActorRefImpl)ref).getActor();
        assertEquals("actor should have its own path", "/actor", actor.getPath());
    }
}