package com.qklabs.actors;

import junit.framework.TestCase;

public class ActorRegistryTest extends TestCase {

    @Override
    public void setUp() {
        ActorRegistry.clear();
    }

    public void testRegisterOneActor() throws Exception {
        ActorRegistry.register("/actorA", ActorA.class);
        assertEquals(ActorA.class, ActorRegistry.lookup("/actorA"));
    }

    public void testRegisterTwoActors() throws Exception {
        ActorRegistry.register("/actorA", ActorA.class);
        ActorRegistry.register("/actorB", ActorB.class);
        assertEquals(ActorA.class, ActorRegistry.lookup("/actorA"));
        assertEquals(ActorB.class, ActorRegistry.lookup("/actorB"));
    }

    public void testRegisterThreeActors() throws Exception {
        ActorRegistry.register("/actorA", ActorA.class);
        ActorRegistry.register("/actorB", ActorB.class);
        ActorRegistry.register("/actorC", ActorC.class);
        assertEquals(ActorA.class, ActorRegistry.lookup("/actorA"));
        assertEquals(ActorB.class, ActorRegistry.lookup("/actorB"));
        assertEquals(ActorC.class, ActorRegistry.lookup("/actorC"));
    }

    public void testClear() throws Exception {
        ActorRegistry.register("/actorA", ActorA.class);
        ActorRegistry.clear();
        assertEquals(null, ActorRegistry.lookup("/actorA"));
    }

    public void testMismatch() throws Exception {
        ActorRegistry.register("/actorA", ActorA.class);
        ActorRegistry.clear();
        assertEquals(null, ActorRegistry.lookup("/actorB"));
    }

    public void testParam() throws Exception {
        ActorRegistry.register("/actorA/*", ActorA.class);
        assertEquals(ActorA.class, ActorRegistry.lookup("/actorA/foobar"));
    }

    public void testParamOverlap() throws Exception {
        ActorRegistry.register("/actor", ActorA.class);
        ActorRegistry.register("/actor/*", ActorB.class);
        assertEquals(ActorA.class, ActorRegistry.lookup("/actor"));
        assertEquals(ActorB.class, ActorRegistry.lookup("/actor/foobar"));
    }

    public void testTwoParams() throws Exception {
        ActorRegistry.register("/actor/*/child/*", ActorA.class);
        assertEquals(ActorA.class, ActorRegistry.lookup("/actor/bob/child/jimmy"));
    }

    public void testOneActorTwoPaths() throws Exception {
        ActorRegistry.register("/actor", ActorA.class);
        ActorRegistry.register("/alias", ActorA.class);
        assertEquals(ActorA.class, ActorRegistry.lookup("/actor"));
        assertEquals(ActorA.class, ActorRegistry.lookup("/alias"));
    }

    public void testTwoActorsOnePath() throws Exception {
        try {
            ActorRegistry.register("/actor", ActorA.class);
            ActorRegistry.register("/actor", ActorB.class);
            fail("should have thrown IllegalStateException");
        } catch (IllegalStateException ignored) {}
    }

    public static final class ActorA extends Actor {}
    public static final class ActorB extends Actor {}
    public static final class ActorC extends Actor {}
}