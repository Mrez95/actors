package com.qklabs.actors;

import junit.framework.TestCase;

public class ActorSystemTest extends TestCase {
    private static StringBuilder builder;

    private static final Object PRESTART_LOCK = new Object();
    private static final Object RECEIVE_LOCK = new Object();
    private static final Object POSTSTOP_LOCK = new Object();

    public static boolean preStarted = false;
    public static boolean postStopped = false;

    private ActorSystem system = new ActorSystem();

    @Override
    public void setUp() {
        builder = new StringBuilder();
        preStarted = false;
        postStopped = false;

        system = new ActorSystem();
        ActorRegistry.clear();
    }

    private void wait(Object obj, int timeout) {
        synchronized (obj) {
            try {
                obj.wait(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void testCreateNonStaticInnerClassActorFails() {
        try {
            system.getOrCreateActor("/illegal", IllegalInnerClassActor.class);
            fail("should have crashed because of non-static inner class");
        } catch (Exception ignored) {}
    }

    public void testActorPreStarted() {
        ActorRef actor = system.getOrCreateActor("/print", PrintActor.class);
        wait(PRESTART_LOCK, 1000);
        assertTrue("actor preStart should be called", preStarted);
    }

    public void testActorPostStopped() {
        ActorRef actor = system.getOrCreateActor("/print", PrintActor.class);
        system.stop(actor);
        wait(POSTSTOP_LOCK, 1000);
        assertTrue("actor postStop should be called", postStopped);
    }

    public void testActorTell() {
        ActorRef actor = system.getOrCreateActor("/print", PrintActor.class);
        actor.tell("Hello!");
        wait(RECEIVE_LOCK, 1000);
        assertEquals("should append \"Hello!\" to builder", "Hello!", builder.toString());
    }

    public void testActorGetWithRegistry() {
        ActorRegistry.register("/print", PrintActor.class);
        system.getOrCreateActor("/print").tell("sup bro?");
        wait(RECEIVE_LOCK, 1000);
        assertEquals("should append \"sup bro?\" to builder", "sup bro?", builder.toString());
    }

    public void testGetActor() {
        ActorRef counter = system.getOrCreateActor("/count", CountActor.class);
        counter.tell(new Increment());

        wait(RECEIVE_LOCK, 1000);

        ActorRef alias = system.getOrCreateActor("/count", CountActor.class);
        alias.tell(new Increment());

        wait(RECEIVE_LOCK, 1000);

        assertEquals("actor should maintain state", "12", builder.toString());
    }

    public void testRecreateActor() {
        ActorRef counter = system.getOrCreateActor("/count", CountActor.class);
        counter.tell(new Increment());
        wait(RECEIVE_LOCK, 1000);

        system.stop(counter);

        counter = system.getOrCreateActor("/count", CountActor.class);
        counter.tell(new Increment());
        wait(RECEIVE_LOCK, 1000);

        assertEquals("actor should reset", "11", builder.toString());
    }

    public void testCantCreateActorsAfterShutdown() {
        try {
            system.shutdown();
            system.getOrCreateActor("/count", CountActor.class);

            fail("create actor after shutdown should throw IllegalStateException");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }
    }

    public void testCantSendMessagesAfterShutdown() {
        try {
            ActorRef counter = system.getOrCreateActor("/count", CountActor.class);
            system.shutdown();
            counter.tell(new Increment());

            fail("send message after shutdown should throw IllegalStateException");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }
    }

    public void testCantStopActorsAfterShutdown() {
        try {
            ActorRef counter = system.getOrCreateActor("/count", CountActor.class);
            system.shutdown();
            system.stop(counter);

            fail("stop actor after shutdown should throw IllegalStateException");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }
    }

    public static class PrintActor extends Actor {
        @Override
        public void preStart() {
            preStarted = true;

            synchronized (PRESTART_LOCK) {
                PRESTART_LOCK.notifyAll();
            }
        }

        @Override
        public void onReceive(Object message, ActorRef sender) {
            if (message instanceof String) {
                builder.append(message);
            }

            synchronized (RECEIVE_LOCK) {
                RECEIVE_LOCK.notifyAll();
            }
        }

        @Override
        public void postStop() {
            postStopped = true;

            synchronized (POSTSTOP_LOCK) {
                POSTSTOP_LOCK.notifyAll();
            }
        }
    }

    public class IllegalInnerClassActor extends Actor {}

    public class Increment {}
    public static class CountActor extends Actor {
        private long counter = 0;
        @Override
        public void onReceive(Object message, ActorRef sender) {
            if (message instanceof Increment) {
                counter++;
                builder.append(counter);
            }

            synchronized (RECEIVE_LOCK) {
                RECEIVE_LOCK.notifyAll();
            }
        }
    }
}
