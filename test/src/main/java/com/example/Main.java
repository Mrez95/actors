package com.example;

import com.qklabs.Actor;
import com.qklabs.ActorRef;
import com.qklabs.ActorSystem;

import java.util.Random;

public class Main {
    public static void main(String[] args) {
        ActorSystem system = new ActorSystem();
        ActorRef actor1 = system.createActor(TestActor.class);
        ActorRef actor2 = system.createActor(TestActor.class);

        for (int i = 0; i < 20; i++) {
            if (i % 2 == 0) {
                System.out.println("sent message to actor1");
                actor1.tell("Hello! " + i);
            } else {
                System.out.println("sent message to actor2");
                actor2.tell("Hey there! " + i);
            }
        }

        while (true);
    }

    public static class TestActor extends Actor {
        Random random = new Random();
        public void onReceive(Object message, ActorRef sender) {
            try {
                Thread.sleep(10 * random.nextInt(10));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(message);
        }
    }
}
