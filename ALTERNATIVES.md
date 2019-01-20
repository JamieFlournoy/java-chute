# Alternatives to Chute

Chute is intended as a simple way for Java threads to safely send messages to each other.

## Other libraries using channel-like abstractions:

There are related Java libraries that go much further, taking ideas from languages like Go and Erlang, and bringing constructs like fibers, channels, and actors to Java (and Java-like JVM languages).

See this Reddit thread for an interesting discussion of some of the alternatives: [Lois: Golang like channels for Java](https://www.reddit.com/r/programming/comments/22zoqj/lois_golang_like_channels_for_java/)

Here are a few of the channel-like libraries for Java, all of which provide a lot more functionality than Chute does.

- Lois (<https://github.com/flipkart-incubator/Lois>) provides "Golang like channels for Java" – bidirectional channels, routines, channel utilities (multiplexing and demultiplexing, and multicasting), and a simple wrapper around an ExecutorService.

- Quasar (<https://github.com/puniverse/quasar> provides "Fibers, Channels and Actors for the JVM". This implementation seems thorough and well-documented.

- Akka (<https://akka.io/>): "Akka is a toolkit for building highly concurrent, distributed, and resilient message-driven applications for Java and Scala".

- JCSP (<https://www.cs.kent.ac.uk/projects/ofa/jcsp/>): Communicating Sequential Processes for Java™ – [CSP for Java](https://www.cs.kent.ac.uk/projects/ofa/jcsp/explain.html) says "JCSP is a (100% pure) Java class library providing a base range of CSP primitives plus a rich set of extensions - some of the latter being experimental at the moment."

### Some reasons why you might want to use Chute instead of these others:

* It's conceptually simple and has good documentation, including Javadocs and [example code][], so you can understand it quickly.
* Its API is designed to make it easy to use correctly, and difficult to use incorrectly.
* It's based on plain old Java SE classes that you already know, and it lets you stick with familiar Java concurrency classes: [Executor][], [Runnable][], and [Callable][].

Some reasons why you might want to use something else:

* Other libraries have more complete implementations of concepts like actors, coroutines, or bidirectional channels.
* Other libraries implement a mathematically verified model of concurrent systems design.
* Other libraries have professional support contracts available, and are used in large enterprise projects.

## Chute compared to non-channel-like things

### What about java.util.stream.Streams?

Java Streams (added in Java 1.8) work well for a particular kind of parallel computing task: similar, CPU-bound computational tasks with no side-effects, operating on a collection that fits in RAM, where a one-shot traversal of the collection is sufficient.

However, if one of the following is true:

* your tasks's data set is too large to fit in RAM, or
* your task needs to write to a log during computation, or
* your tasks may fail and need retrying, or
* your tasks may fail and you want to send an error object to an error processing system to describe each failure, or
* your task needs to allow for real-time adjustment of control inputs, such as concurrency level, timeout length, or task complexity (for load shedding), or
* your task needs to to fork copies of elements into a separate part of a processing stream (not partitioning, but cloning), or
* your task needs to maintain shared state across all threads, such as a rate-limiter or a progress tracker or a checkpoint-saver that would let you resume computation in the event of a crash, or
* your tasks involve a combination of I/O and computation, where using the same pool of threads for I/O and computation could leave the CPU idle,

then Java Streams are probably not what your application needs.

A Chute may be useful in any of these types of application.

If you have an application that is almost well-suited to Streams, you might find that combining Streams and a Chute could work. For example, you could combine `Spliterators.spliteratorUnknownSize` with `Chutes.asIterable(ChuteExit)`, feeding a Stream-based processing pipeline from a ChuteExit. Compare this to the complexity of the custom I/O Spliterator described in [Faster parallel processing in Java using Streams and a spliterator](https://www.airpair.com/java/posts/parallel-processing-of-io-based-data-with-java-streams).

### What about BlockingQueue?

A BlockingChute has chute-closing semantics that work properly. You can roll your own queue-plus-closing with java.util.concurrent.* classes, but it's not trivial to do.

### What about ListenableExecutorService / ListenableFuture?

These are very useful for tasks that decompose into a directed acyclic graph of I/O and computation tasks, especially if these are dissimilar in nature – fetching data from various backends, doing a bunch of heterogeneous computation on results and maybe doing more I/O based on that. A Chute doesn't really help with this sort of application.

However, if you have a collection of fairly similar computational tasks which work well with a Chute, and a few of those might result in activity that looks like a DAG of blocking I/O and result processing, then you could use a Chute for the computational part and ListenableExecutorService and/or ListenableFuture for the I/O part. 




[Executor]: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Executor.html

[Runnable]: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Runnable.html

[Callable]: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Callable.html

[Queue]: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Queue.html

[example code]: OVERVIEW.md#example-code

