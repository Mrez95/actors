package com.qklabs.actor;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import sun.plugin.dom.exception.InvalidStateException;

public class ActorSystemTest {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    private static final Object PRESTART_LOCK = new Object();
    private static final Object RECEIVE_LOCK = new Object();
    private static final Object POSTSTOP_LOCK = new Object();

    public static boolean preStarted = false;
    public static boolean postStopped = false;

    private ActorSystem system = new ActorSystem();

    @Before
    public void setup() {
        System.setOut(new PrintStream(out));
        preStarted = false;
        postStopped = false;

        system = new ActorSystem();
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

    @Test(expected = RuntimeException.class)
    public void testCreateNonStaticInnerClassActorFails() {
        system.getOrCreateActor(IllegalInnerClassActor.class, "/illegal");
    }

    @Test
    public void testActorPreStarted() {
        ActorRef actor = system.getOrCreateActor(PrintActor.class, "/print");
        wait(PRESTART_LOCK, 1000);
        assertTrue("actor preStart should be called", preStarted);
    }

    @Test
    public void testActorPostStopped() {
        ActorRef actor = system.getOrCreateActor(PrintActor.class, "/print");
        system.stop(actor);
        wait(POSTSTOP_LOCK, 1000);
        assertTrue("actor postStop should be called", postStopped);
    }

    @Test
    public void testActorTell() {
        ActorRef actor = system.getOrCreateActor(PrintActor.class, "/print");
        actor.tell("Hello!");
        wait(RECEIVE_LOCK, 1000);
        assertEquals("actor should print \"Hello!\" to stdout", "Hello!", out.toString());
    }

    @Test
    public void testActorPathNormalized() {
        ActorRef actor = system.getOrCreateActor(PrintActor.class, "//hello/../print");
        assertEquals("actor should have normalized path", "/print", actor.getPath());
    }

    @Test
    public void testGetActor() {
        ActorRef counter = system.getOrCreateActor(CountActor.class, "/count");
        counter.tell(new Increment());

        wait(RECEIVE_LOCK, 1000);

        ActorRef alias = system.getOrCreateActor(CountActor.class, "/count");
        alias.tell(new Increment());

        wait(RECEIVE_LOCK, 1000);

        assertEquals("actor should maintain state", "12", out.toString());
    }

    @Test
    public void testRecreateActor() {
        ActorRef counter = system.getOrCreateActor(CountActor.class, "/count");
        counter.tell(new Increment());
        wait(RECEIVE_LOCK, 1000);

        system.stop(counter);

        counter = system.getOrCreateActor(CountActor.class, "/count");
        counter.tell(new Increment());
        wait(RECEIVE_LOCK, 1000);

        assertEquals("actor should reset", "11", out.toString());
    }

    @Test
    public void testNormalizePathExtraSlashes() {
        String str = "//hey/there//sup";
        assertEquals("/hey/there/sup", system.normalizePath(str));
    }

    @Test
    public void testNormalizePathCurrentDir() {
        String str = "/hey/./there";
        assertEquals("/hey/there", system.normalizePath(str));
    }

    @Test
    public void testNormalizePathParentDir() {
        String str = "/hey/../there";
        assertEquals("/there", system.normalizePath(str));
    }

    @Test(expected = InvalidStateException.class)
    public void testCantCreateActorsAfterShutdown() {
        system.shutdown();
        system.getOrCreateActor(CountActor.class, "/count");
    }

    @Test(expected = InvalidStateException.class)
    public void testCantSendMessagesAfterShutdown() {
        ActorRef counter = system.getOrCreateActor(CountActor.class, "/count");
        system.shutdown();
        counter.tell(new Increment());
    }

    @Test(expected = InvalidStateException.class)
    public void testCantStopActorsAfterShutdown() {
        ActorRef counter = system.getOrCreateActor(CountActor.class, "/count");
        system.shutdown();
        system.stop(counter);
    }

    @Test
    public void testCreateActorWithNoRootPrefix() {
        ActorRef actor = system.getOrCreateActor(Actor.class, "test");
        assertEquals("actor path should have root prefix", "/test", actor.getPath());
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
                System.out.print(message);
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
                System.out.print(counter);
            }

            synchronized (RECEIVE_LOCK) {
                RECEIVE_LOCK.notifyAll();
            }
        }
    }
}
