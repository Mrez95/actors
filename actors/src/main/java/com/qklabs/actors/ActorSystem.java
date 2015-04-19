package com.qklabs.actors;

import android.content.res.Resources;
import android.net.Uri;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ActorSystem {

    private static final String LOG_TAG = "ActorSystem";
    static final Logger LOG = Logger.getLogger(LOG_TAG);

    static final int N_THREADS = 8;

    private static final EmptyActor EMPTY_ACTOR = new EmptyActor();

    @SuppressWarnings("FieldCanBeLocal")
    private final ExecutorService mExecutor;
    private final MessageQueue[] mQueues;
    private final Map<Actor, MessageQueue> mActorsQueueMap;
    private final Map<String, Actor> mActors;

    private boolean mIsStopped = false;

    public ActorSystem() {
        mExecutor = Executors.newFixedThreadPool(N_THREADS);
        mQueues = new MessageQueue[N_THREADS];
        mActorsQueueMap = new HashMap<>();
        mActors = new HashMap<>();

        initializeQueues();
        for (MessageQueue queue : mQueues) {
            mExecutor.execute(new ActorsQueueRunnable(queue));
        }
    }

    private void initializeQueues() {
        for (int i = 0; i < N_THREADS; i++) {
            mQueues[i] = new MessageQueue();
        }
    }

    /**
     * Shuts down the actor system.
     *
     * Existing actors in the system will not be notified of this; they will just stop receiving
     * events.
     */
    public void shutdown() {
        // Shut down executor
        mExecutor.shutdownNow();
        try {
            mExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.warning("Interrupted while shutting down ActorSystem");
        }

        mActors.clear();
        mActorsQueueMap.clear();

        mIsStopped = true;
    }

    private boolean isStopped() {
        return mIsStopped;
    }

    /**
     * Retrieves an actor. The path is treated as a relative path from root and is normalized before
     * being set. If the actor corresponding to the given path doesn't exist, it will be created.
     *
     * @param path the location of the actor in the system
     * @param cls the actor's class
     */
    public ActorRef getOrCreateActor(String path, Class<? extends Actor> cls) {
        if (isStopped()) {
            throw new IllegalStateException("Cannot create actors after shutdown() is called");
        }

        final Actor actor;
        final ActorRef result;
        if (mActors.containsKey(path)) {
            actor = mActors.get(path);
            result = new ActorRefImpl(this, actor, path);
        } else {
            actor = create(cls);
            mActors.put(path, actor);
            result = new ActorRefImpl(this, actor, path);
            actor.setSelf(result);
            // Bind the actor to a thread in the actor system
            bind(actor);
        }
        return result;
    }

    public ActorRef getOrCreateActor(String path) {
        Class<? extends Actor> cls = ActorRegistry.lookup(path);
        if (cls == null) {
            String msg = "No actor class was registered for the path " + path;
            throw new Resources.NotFoundException(msg);
        } else {
            return getOrCreateActor(path, cls);
        }
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
        if (isStopped()) {
            throw new IllegalStateException("Cannot send messages to an actor after shutdown() is " +
                    "called");
        }

        if (target instanceof EmptyActor) {
            LOG.info("Message sent to empty actor: " + message);
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
        if (isStopped()) {
            throw new IllegalStateException("Cannot stop an actor after shutdown() is called");
        }

        Actor targetActor = ((ActorRefImpl)target).getActor();
        MessageQueue queue = mActorsQueueMap.get(targetActor);
        queue.stop(targetActor);
        mActors.remove(target.getPath());
    }

    public ActorRef getEmptyActorRef() {
        return new ActorRefImpl(this, EMPTY_ACTOR, "/empty");
    }

    private static class EmptyActor extends Actor {}
}
