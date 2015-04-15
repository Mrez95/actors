# actors
Minimal actor library for Android.

This is a work in progress! Inspired by [Akka](http://akka.io/).

### Purpose
Akka is a great library for concurrent programming with Actors. However, isn't a good choice for Android:

1. **Scala**. Akka is written in Scala, and it is difficult to use Scala to build Android apps. Although Scala is a JVM language, Android tooling doesn't officially support Scala. Frequent updates to Android tools (such as Android studio) leave Scala Android developers behind.
2. **Heavyweight**. The Akka library plus the Scala runtime library are over 9mb, which is large for Android apps. The application size limit for Android apps is 50mb, but 10mb or less is preferred.

### Features

##### Asynchronous
Messages are handled asynchronously, and many actors are multiplexed onto several threads.

##### Sequential events
Actors handle messages sequentially. This means that locks and `synchronized` blocks are (usually) unnecessary.

### Sample Programs
There is a sample program available in `actors/examples`. It's pretty fun. It shows that actors are asynchronous, but process their messages in order. Try it out!

![](/screenshots/example-run.png)
