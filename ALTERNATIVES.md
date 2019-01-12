# Alternatives to Chute

Chute is intended as a simple way for Java threads to safely send messages to each other.

There are related Java libraries that go much further, taking ideas from languages like Go and Erlang, and bringing constructs like fibers, channels, and actors to Java (and Java-like JVM languages).

See this Reddit thread for an interesting discussion of some of the alternatives: [Lois: Golang like channels for Java](https://www.reddit.com/r/programming/comments/22zoqj/lois_golang_like_channels_for_java/)

Here are a few of the channel-like libraries for Java, all of which provide a lot more functionality than Chute does.

- Lois (<https://github.com/flipkart-incubator/Lois>) provides "Golang like channels for Java" – bidirectional channels, routines, channel utilities (multiplexing and demultiplexing, and multicasting), and a simple wrapper around an ExecutorService.

- Quasar (<https://github.com/puniverse/quasar> provides "Fibers, Channels and Actors for the JVM". This implementation seems thorough and well-documented.

- Akka (<https://akka.io/>): "Akka is a toolkit for building highly concurrent, distributed, and resilient message-driven applications for Java and Scala".

- JCSP (<https://www.cs.kent.ac.uk/projects/ofa/jcsp/>): Communicating Sequential Processes for Java™ – [CSP for Java](https://www.cs.kent.ac.uk/projects/ofa/jcsp/explain.html) says "JCSP is a (100% pure) Java class library providing a base range of CSP primitives plus a rich set of extensions - some of the latter being experimental at the moment."

 