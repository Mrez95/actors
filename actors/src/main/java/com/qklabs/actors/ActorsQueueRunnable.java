package com.qklabs.actors;

class ActorsQueueRunnable implements Runnable {
    private final MessageQueue mQueue;

    public ActorsQueueRunnable(MessageQueue queue) {
        mQueue = queue;
    }

    @Override
    public void run() {
        while (true) {
            mQueue.processEvent();
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
        }
    }
}
