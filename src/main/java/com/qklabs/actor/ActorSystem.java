package com.qklabs.actor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ActorSystem {
    private static final int N_THREADS = 8;
    private static final EmptyActor EMPTY_ACTOR = new EmptyActor();
    private static final Logger LOG = Logger.getLogger("ActorSystem");

    @SuppressWarnings("FieldCanBeLocal")
    private final ExecutorService mExecutor;
    private final MessageQueue[] mQueues;
    private final Map<Actor, MessageQueue> mActorsQueueMap;
    private final Map<String, Actor> mActors;

    public ActorSystem() {
        mExecutor = Executors.newFixedThreadPool(N_THREADS);
        mQueues = new MessageQueue[N_THREADS];
        mActorsQueueMap = new HashMap<>();
        mActors = new HashMap<>();

        initializeQueues();
        for (MessageQueue queue : mQueues) {
            mExecutor.execute(new BlockingActorQueue(queue));
        }
    }

    private void initializeQueues() {
        for (int i = 0; i < N_THREADS; i++) {
            mQueues[i] = new MessageQueue();
        }
    }

    /**
     * Returns the actor for the specified path. If the actor for the specified path doesn't already
     * exist, it is created.
     *
     * @param cls the actor's class
     * @param path the location of the actor in the system
     */
    public ActorRef getOrCreateActor(Class<? extends Actor> cls, String path) {
        String oldPath = path;
        path = normalizePath(path);
        if (!Objects.equals(oldPath, path)) {
            LOG.warning("getOrCreateActor: normalizing paths. was " + oldPath + " is now " + path);
        }

        final Actor actor;
        if (mActors.containsKey(path)) {
            actor = mActors.get(path);
        } else {
            actor = create(cls);
            mActors.put(path, actor);
            // Bind the actor to a thread in the actor system
            bind(actor);
        }
        return new ActorRefImpl(this, actor, path);
    }

    String normalizePath(String path) {
        return Paths.get(path).normalize().toString();
    }

    private void bind(Actor actor) {
        MessageQueue queue = getNextQueue();
        mActorsQueueMap.put(actor, queue);
        queue.start(actor);
    }

    private final Random mQueueSelectionRandom = new Random();
    private MessageQueue getNextQueue() {
        return mQueues[mQueueSelectionRandom.nextInt(N_THREADS)];
    }

    private Actor create(Class<? extends Actor> cls) {
        try {
            Constructor ctor = cls.getConstructor();
            return (Actor)ctor.newInstance();
        } catch (NoSuchMethodException e) {
            String msg = String.format(
                    "Actor %s must have empty constructor.\n" +
                    "Note that non-static inner classes can't have an empty constructor, so " +
                    "an Actor can't currently be a non-static inner class.", cls.getName());
            throw new RuntimeException(msg, e);
        } catch (InvocationTargetException|InstantiationException|IllegalAccessException e) {
            throw new RuntimeException("Could not create actor", e);
        }
    }

    void send(Actor target, Object message, ActorRef sender) {
        if (target instanceof EmptyActor) {
            // Log.w(TAG, "Message sent to empty actor: " + message");
            System.err.println("Message sent to empty actor: " + message);
        } else {
            if (message instanceof PoisonPill) {
                MessageQueue queue = mActorsQueueMap.get(target);
                queue.stop(target);
                mActorsQueueMap.remove(target);
            } else {
                MessageQueue queue = mActorsQueueMap.get(target);
                queue.sendMessage(target, message, sender);
            }
        }
    }

    /**
     * Stops an actor. `postStop` will be called on the Actor's thread asynchronously.
     * @param target the actor to stop
     */
    public void stop(ActorRef target) {
        Actor targetActor = ((ActorRefImpl)target).getActor();
        MessageQueue queue = mActorsQueueMap.get(targetActor);
        queue.stop(targetActor);
        mActors.remove(target.getPath());
    }

    public ActorRef getEmptyActorRef() {
        return new ActorRefImpl(this, EMPTY_ACTOR, "/empty");
    }

    private static class BlockingActorQueue implements Runnable {
        private final MessageQueue mQueue;

        public BlockingActorQueue(MessageQueue queue) {
            mQueue = queue;
        }

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            while (true) {
                mQueue.processEvent();
            }
        }
    }

    private static class EmptyActor extends Actor {}
}
