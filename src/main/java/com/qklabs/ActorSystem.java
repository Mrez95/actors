package com.qklabs;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActorSystem {
    private static final int N_THREADS = 8;

    @SuppressWarnings("FieldCanBeLocal")
    private final ExecutorService mExecutor;
    private final MessageQueue[] mQueues;
    private final Map<Actor, MessageQueue> mActorsQueueMap;

    public ActorSystem() {
        mExecutor = Executors.newFixedThreadPool(N_THREADS);
        mQueues = new MessageQueue[N_THREADS];
        mActorsQueueMap = new HashMap<>();

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

    ActorRef actorOf(Class<Actor> cls) {
        Actor actor = create(cls);

        // Bind the actor to a thread in the actor system
        bind(actor);

        return new ActorRefImpl(this, actor);
    }

    private void bind(Actor actor) {
        Random random = new Random();
        MessageQueue queue = mQueues[random.nextInt(N_THREADS)];

        mActorsQueueMap.put(actor, queue);
        queue.start(actor);
    }

    private Actor create(Class<Actor> cls) {
        try {
            Constructor ctor = cls.getConstructor();
            return (Actor)ctor.newInstance();
        } catch (NoSuchMethodException e) {
            String msg = String.format("Actor %s must have empty constructor", cls.getName());
            throw new RuntimeException(msg, e);
        } catch (InvocationTargetException |InstantiationException|IllegalAccessException e) {
            throw new RuntimeException("Could not create actor", e);
        }
    }

    void send(Actor target, Object message, ActorRef sender) {
        if (message instanceof PoisonPill) {
            MessageQueue queue = mActorsQueueMap.get(target);
            queue.stop(target);
            mActorsQueueMap.remove(target);
        } else {
            MessageQueue queue = mActorsQueueMap.get(target);
            queue.sendMessage(target, message, sender);
        }
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
}
