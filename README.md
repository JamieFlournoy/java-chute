# Pervasive Code's Java Chute

This library includes classes for simplifying producer/consumer parallel processing using a channel-like data structure called a Chute.

## Chute?

A Chute is a closable conduit from producers to consumers  of objects of a given type. It is unidirectional and typed, similar to a bounded queue but with a substantially smaller API than [java.util.Queue][Queue].

The API is deliberately limited to operations that are suited to a pool of competing workers. Additionally, the producer and consumer interfaces are separate (`ChuteEntrance` and `ChuteExit`), easing implementation of delegating wrapper classes that enhance basic chutes. Several examples of such classes are included with this library:

* a **multiplexer**, allowing multiple producers to feed a single ChuteEntrance, with the ChuteEntrance only becoming closed when all of the producers have signaled that they are finished;
* a **transforming entrance** and a **transforming exit,** allowing flexibility in deciding whether producers or consumers execute the transformation function;
* an **Iterable** implementation that represents a ChuteExit in a way that is easy to use in a `foreach` loop, and which is suitable for such use by multiple workers, each of which will see their own subset of the Chute's contents;
* executable **workers** that process elements taken from a ChuteExit, putting the results into a ChuteEntrance; and
* a wrapper that allows a **nonblocking listener** to handle elements from a large number of ChuteExits.

## Overview of included classes

Javadocs are available on `javadoc.io`:

[![Javadocs](https://www.javadoc.io/badge/com.pervasivecode/chute.svg)](https://www.javadoc.io/doc/com.pervasivecode/chute)

See the separate [OVERVIEW.md](OVERVIEW.md) file for a description of what interfaces and classes are included.
(Overview content is taken from class Javadoc comments, so there's no need to read both.)

## Alternatives to this library

A list of [alternatives to this library](ALTERNATIVES.md) is included to help you determine whether this library is right for you.

## Including it in your project

Use groupId `com.pervasivecode`, name `chute`, version `0.10` in your build tool of choice.


### Gradle Example

If you are using Gradle 4.x, put this in your build.properties file:

```
// in your build.gradle's repositories {} block:
    mavenCentral();

// in your build.gradle's dependencies {} block:
    implementation 'com.pervasivecode:chute:0.10'

    // or, if you prefer the separated group/name/version syntax:
    implementation group: 'com.pervasivecode', name: 'chute', version: '0.10'
```


## How to use it in your code

See the [Example Code][] section in [OVERVIEW.md](OVERVIEW.md) for details.

## Contributing

See [DEVELOPERS.md](DEVELOPERS.md) and [GRADLE_INTRO.md](GRADLE_INTRO.md) if you want to build and hack on the code yourself.


## Copyright and License

Copyright © 2018 Jamie Flournoy.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.


[example code]: OVERVIEW.md#example-code
