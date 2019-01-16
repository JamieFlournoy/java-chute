#  Code Overview

If you prefer Javadocs, they are available on `javadoc.io`:

[![Javadocs](https://www.javadoc.io/badge/com.pervasivecode/chute.svg)](https://www.javadoc.io/doc/com.pervasivecode/chute)

## Interfaces

### [Chute](src/main/java/com/pervasivecode/utils/concurrent/chute/Chute.java)

A `Chute` is a closable conduit between producers and consumers of objects of a given type.

This interface is just the combination of the `ChuteEntrance` and `ChuteExit` interfaces.

### [ChuteEntrance](src/main/java/com/pervasivecode/utils/concurrent/chute/ChuteEntrance.java)

The input side of a chute, allowing callers to put elements into the chute, or to close the chute so that no more elements can be put into it.

### [ChuteExit](src/main/java/com/pervasivecode/utils/concurrent/chute/ChuteExit.java)

The output side of a chute, allowing callers to take elements from a chute until it is closed.

### [ListenableChute](src/main/java/com/pervasivecode/utils/concurrent/chute/ListenableChute.java)

A ListenableChute is a `Chute` that has a `ListenableChuteExit` rather than just a regular `ChuteExit`.

### [ListenableChuteExit](src/main/java/com/pervasivecode/utils/concurrent/chute/ListenableChuteExit.java)

A `ChuteExit` that allows listeners to be called when there is something to take from the `ChuteExit`, or when the `ChuteExit` is closed and empty.

Using a listener avoids the need to have one thread per `ChuteExit` blocked waiting for the next element. Instead, a single thread can process elements as they become available from multiple unrelated `ChuteExit`s.

## Implementation Classes

### [BufferingChute](src/main/java/com/pervasivecode/utils/concurrent/chute/BufferingChute.java)

A `Chute` based on a [BlockingQueue](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/BlockingQueue.html), providing a fixed-size nonzero-capacity buffer that holds elements that have been put into the `ChuteEntrance` but not yet taken from the `ChuteExit`.

### Chutes

Factory methods for representing `Chutes`, `ChuteEntrances`, and `ChuteExits` in useful ways.

### SynchronousMultiplexer

A SynchronousMultiplexer provides multiple `ChuteEntrance` instances which all feed into a single `ChuteEntrance`, all with the same element type. When all of the provided `ChuteEntrances` are closed, the output `ChuteEntrance` will be closed.

### Workers

Factory methods for executable workers that process elements taken from `Chute`s.

## Example Code

### [DinerExample](src/examples/java/com/pervasivecode/utils/concurrent/chute/example/DinerExample.java)

This example program shows how a single thread, representing a very busy waiter, can handle a large number of production queues, as long as the single thread doesn't have to block waiting for input, because it uses ListenableChute.


### [ParallelVirusScannerExample](src/examples/java/com/pervasivecode/utils/concurrent/chute/example/ParallelVirusScannerExample.java)

This example program is a virus scanner. It reads all of the individual files from an archive, and scans each one for viruses using a predefined set of virus signatures.

When it finds a virus, it reports which file was being scanned and which virus or viruses were found.

Optionally, it will also print the context of the match, showing the byte position where the match occurred and the bytes before and after the match.

Well, actually, this is a _fake_ virus scanner. The "archive" is the The Project Gutenberg EBook of Moby Dick; the "files" are individual chapters, and the "virus signatures" are the words "ahoy", "Ahoy", "gangway", "tallow", "Yonder", and "yonder". The context shown in the verbose output includes the whole line that includes the "signature". See examples.feature for what the real output looks like.

The "archive" is split into files in a separate thread that searches the archive content for "file headers" (which are actually just chapter headings).

The "files" are also searched in parallel.

See [SingleThreadedVirusScannerExample](src/examples/java/com/pervasivecode/utils/concurrent/chute/example/SingleThreadedVirusScannerExample.java) for a simpler, single-threaded implementation to help clarify what the parallel code does.





