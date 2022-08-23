# Kotlin Coroutines Codelab

Welcome to this Kotlin Coroutines Codelab! Here you'll learn:

- Why coroutines and what they are
- The three standard ways to execute coroutines
- What dispatchers do and when they're needed
- How to make coroutines from blocking code and callbacks
- What is structured concurrency
- How to handle exceptions
- How to support cancellation

## The exercises

The exercises in this repository are organized in branches and are supposed to be tackled in order. The solution for
each exercise can be found in its corresponding branch.

1. [Executing coroutines](../../tree/01-executing_coroutines) ([solution](../../tree/01-solution))
2. [Blocking code to coroutines](../../tree/02-blocking2coroutines) ([solution](../../tree/02-solution))
3. [Callbacks to coroutines](../../tree/03-callbacks2coroutines) ([solution](../../tree/03-solution))
4. [Structured concurrency](../../tree/04-structured_concurrency) ([solution](../../tree/04-solution))
5. [Exception handling](../../tree/05-exception_handling) ([solution](../../tree/05-solution))
6. [Cooperative cancellation](../../tree/06-cooperative_cancellation) ([solution](../../tree/06-solution))

## Why coroutines

As with many other asynchronous programming patterns and libraries, the goal of Kotlin Coroutines is to make concurrent
code easier to write and understand. This goal is pursued by allowing developers to write non-blocking code that looks
like sequential, blocking code.

Consider the following example:

```kotlin

suspend fun fetchAndCache(): SomeData? {
    try {
        val data = getFromNetwork()
        storeInDb(data)
        return data
    } catch (e: IOException) {
        return getFromDb()
    }
}

suspend fun getFromNetwork(): SomeData
suspend fun getFromDb(): SomeData?
suspend fun storeInDb(data: SomeData)
```

If it wasn't for the `suspend` modifier sprinkled on it, this code would look and work like regular blocking code
performing I/O operations. However, thanks to the native support in the Kotlin compiler for `suspend` functions, these
function don't block the thread they're called from.

Another benefit of coroutines is that they follow the **structured concurrency** paradigm. In the structured concurrency
world, asynchronous computations always live within a parent which delimits the computations' lifetime.

Following this paradigm makes it more difficult to leak computations, meaning that it makes it easier to avoid having
code running longer than its intended lifetime. In the Kotlin world, the parent is called _scope_ and is provided in the
standard library as `CoroutineScope`.

## What is a coroutine

From the [doc](https://kotlinlang.org/docs/coroutines-basics.html):

> A coroutine is an instance of suspendable computation.

Coroutines are often promoted as lightweight threads and, from a conceptual standpoint, the comparison holds. Both are
instances of concurrent computations that, at any given point in time, can be running or paused. However, while a thread
is an actual runtime entity in the JVM linked to an operating system thread, a coroutine is a compile-time construct.

Since programming is not magic (even though sometimes it looks like it), the code of a coroutine still needs a thread to
run, just like any other piece of code. At any point in time, a running coroutine is executing on a thread but it's not
tied to it. After suspension, it can be resumed on another thread, so the same coroutine can run on different threads
during its lifetime.

Note: a `suspend` function is not a coroutine. It is a function that can suspend the coroutine it's called from.

**Let's now jump to the [first exercise](../../tree/01-executing_coroutines) and start executing coroutines.**
