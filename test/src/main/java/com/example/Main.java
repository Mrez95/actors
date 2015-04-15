package com.example;

import com.qklabs.actor.Actor;
import com.qklabs.actor.ActorRef;
import com.qklabs.actor.ActorSystem;

import java.util.Random;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = new ActorSystem();
        ActorRef actor1 = system.getOrCreateActor(TestActor.class, "/actor1");
        ActorRef actor2 = system.getOrCreateActor(TestActor.class, "/actor2");

        for (int i = 0; i < 20; i++) {
            if (i % 2 == 0) {
                actor1.tell("Hello! " + i);
            } else {
                actor2.tell("Hey there! " + i);
            }
        }

        Thread.sleep(5000);

        System.out.println("Too late!");
        System.exit(0);
    }

    public static class TestActor extends Actor {
        Random random = new Random();
        public void onReceive(Object message, ActorRef sender) {
            try {
                Thread.sleep(10 * random.nextInt(100));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(message);
        }
    }
}
