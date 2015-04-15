package com.example;

import com.qklabs.actor.Actor;
import com.qklabs.actor.ActorRef;
import com.qklabs.actor.ActorSystem;

import java.util.Random;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = new ActorSystem();
        ActorRef actor1 = system.getOrCreateActor(PrintActor.class, "/actor1");
        ActorRef actor2 = system.getOrCreateActor(PrintActor.class, "/actor2");

        for (int i = 0; i < 20; i++) {
            if (i % 2 == 0) {
                actor1.tell("Hello! " + i);
            } else {
                actor2.tell("Hey there! " + i);
            }
        }

        Thread.sleep(3000);

        System.out.println("\nToo late!");
        System.exit(0);
    }

    public static class PrintActor extends Actor {
        Random random = new Random();

        public void onReceive(Object message, ActorRef sender) {
            if (message instanceof String) {
                sleep(8 * random.nextInt(100));
                System.out.println(getPath() + ": " + message);
            }
        }

        private void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (Exception ignored) {}
        }
    }
}
