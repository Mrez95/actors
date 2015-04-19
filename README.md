# actors
Minimal actor library for Android.

This is a work in progress! Heavily inspired and indebted to the [Akka](http://akka.io/) project.

## Purpose
Actors are great for concurrent programming. Events are handled in actors sequentially, so there is no need to synchronize. However, since actors are modular and don't share any state, they can process their events in a multithreaded fashion without any additional complexity.

[Akka](http://akka.io) is a great library for concurrent programming with actors. However, isn't a good choice for Android:

1. **Scala**. Akka is written in Scala, and it is difficult to use Scala to build Android apps. Although Scala is a JVM language, Android tooling doesn't officially support Scala. Frequent updates to Android tools (such as Android studio) leave Scala Android developers behind.
2. **Heavyweight**. The Akka library plus the Scala runtime library are over 9mb, which is large for Android apps. The application size limit for Android apps is 50mb, but 10mb or less is preferred.

## How actors work
Actors are addressable event handlers. Each actor corresponds to a path (much like how a webpage corresponds to a URL). They can send and receive messages, which are just Java objects. They can also retrieve other actors to send messages to by looking them up via their paths. Here is a full example:

```java
import com.example.app.data.DB;
import com.qklabs.actor.Actor;
import java.util.Set;

public class UserActor extends Actor {
  Set<String> mFriendIds;
  String mPath;

  // Called when the actor is first created
  @Override
  public void preStart() {
    mPath = getPath(); // "/user/{user_id}"
    mFriendIds = DB.load(mPath + "/friends");
  }
  
  @Override
  public void onReceive(Object o, ActorRef sender) {
    if (o instanceof FriendRequest) {
      FriendRequest request = (FriendRequest)o;
      if (!"Vikrem".equals(request.name)) {
        // Tell the sender that we accept the friend request
        sender.tell(new Accept(), self());
        // Add the user id of our new friend to our friends set
        mFriendIds.add(request.userId);
      } else {
        // Don't be friends with Vikrems
        sender.tell(new Deny(), self());
        
    } else if (o instanceof PartyStarted) {
      // Tell all our friends how excited we are about the party
      for (String userId : mFriendIds) {
        // Create the actor for our friend by path
        String path = "/user/" + userId;
        ActorRef friend = MyApplication.getActorSystem().getOrCreateActor(path);
        friend.tell(new ChatMessage("AWW YYEE"), self());
      }
    }
  }
  
  // Called when the actor is stopped.
  @Override
  public void postStop() {
    DB.persist(mPath + "/friends", mFriends);
  }
  
  // A user has requested to be our friend.
  public static class FriendRequest {
    public String name;
    public String userId;
  }
  
  // Replies for FriendRequest
  public static class Accept {}
  public static class Deny {}
  
  // A party has started
  public static class PartyStarted {}
  
  // Send a chat message to your friend
  public static class ChatMessage {
    public String message;
    public ChatMessage(String message) {
      this.message = message;
    }
  }
}
```

### Creating actors
The `ActorSystem` is where actors are created, using the method `getOrCreateActor(String path, Class<? extends Actor> cls)` (or the simpler `getOrCreateActor(String path)`&mdash;see below). The actor system will try to look up an existing actor for the path first; if the actor for that path doesn't already exist, a fresh one of the given class will be created. An `ActorRef` is returned, which you can use to send messages to the actor.

```java
// Get the actor system
ActorSystem system = new ActorSystem();
// Get a reference to the MyActor for the path "myActor"
ActorRef actor = system.getOrCreateActor("/myActor", MyActor.class);
// Send it a message. See below for more information
actor.tell(new MyMessage());
```

#### The actor lifecycle

> Note: The `shutdown` method on `ActorSystem` doesn't currently call `postStop` on every actor in the system as it should.

Creation is only the first part of the actor lifecycle. The whole lifecycle is as follows:

1. `preStart` is called asynchronously on creation.
2. `onReceive(Object, ActorRef)` is called asynchronously for every message received via a `tell` on an `ActorRef` that points to the actor.
3. `postStop` is called asynchronously after the actor has stopped receiving messages.

#### ActorSystem is a heavy weight class
`ActorSystem` is a relatively heavy weight class, since it spins up a bunch of threads to run the actors on. You should only need one per application:
```java
public class MyApplication extends Application {
  private static final sActorSystem = new ActorSystem();
  public static ActorSystem getActorSystem() {
    return sActorSystem;
  }
  // ...
}
```

#### Using `ActorRegistry`
You can configure `ActorSystem` to know which class to use when creating actors for a path using `ActorRegistry`:

```java
ActorRegistry.register("/user/*", UserActor.class);
ActorRegister.register("/events", EventsActor.class);
ActorRegistry.register("/events/*", EventActor.class);

ActorSystem system = new ActorSystem();
ActorRef user = system.getOrCreateActor("/user/abc123"); // returns a ref to a UserActor for user abc123
ActorRef events = system.getOrCreateActor("/events"); // returns a ref to a EventActor
```

Calls to `ActorSystem.getOrCreateActor(String path)` will fail if the path doesn't match anything in the registry.

#### Temporary actors
> Note: This has not yet been implemented.

Some actors don't need a well-defined path such as "/user/abc123", and you just want to do some quick-and-dirty message passing. For this you can use `ActorSystem.getOrCreateActor(Class<? extends Actor> cls)`. This just creates a temporary actor with the path "/tmp/{random_uuid}".

### Talking to actors
The way actors handle events asynchronously is by receiving messages through a reference. You can send a message to an actor like this:

```java
ActorRef actor = system.getOrCreateActor("/actor");
actor.tell(new Message());
```

You can also set the sender in the `tell` method, in case you want to be an arbiter for communication between two other actors:

```java
ActorRef producer = system.getOrCreateActor("/produce");
ActorRef consumer = system.getOrCreateActor("/consume");

// Subscribe the consumer actor to the producer actor. The producer will add 
// the "sender" ActorRef to its list of subscribers and keep it updated.
producer.tell(new Subscribe(), consumer);
```

#### Don't try to call methods on actors
You should never construct an actor directly. This is one way to break the message passing encapsulation. Communicate with actors via `tell`ing it messages using the `ActorRef` you get from `getOrCreateActor`.

```java
public class BadClass {
  public BadClass() {
    // bad --- do not do this!
    MyActor actor = new MyActor();
    actor.onReceive(new Message(), null);
  }
}
```

#### Calling back to non-actors
If you need to call back to somewhere that isn't an actor, pass it an `Init` message with a callback field. Do not create a public method on an Actor and call it directly.

```java
public class MyActor extends Actor {
  private Callback mCallback;
  
  @Override
  public void onReceive(Object o, ActorRef sender) {
    if (o instanceof Init) {
      // Save the callback for later
      mCallback = ((Init)o).callback;
    } else if (o instanceof Request) {
      Request request = (Request) o;
      mCallback.onResult(handleRequest(request));
    }
  }
  
  private Result handleRequest(Request request) {
    // ...
  }
  
  public static class Init {
    public Callback callback;
    public Init(Callback callback) {
      this.callback = callback;
    }
  }
  public static class Request {
    // ...
  }
  public static class Result {
    // ...
  }
}
```

### Stopping actors
Actors can be stopped a few ways:

```java
ActorRef actor1 = system.getOrCreateActor("/actor1");
ActorRef actor2 = system.getOrCreateActor("/actor2");

// Can stop via the actor system
system.stop(actor1);

// Or you can send a PoisonPill to the actor itself
actor2.tell(new PoisonPill());

// Now actor1 and actor2 are stopped asynchronously.
```
