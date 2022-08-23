# Kotlin Coroutines Codelab

Welcome to this Kotlin Coroutines Codelab! Here you'll learn:

- Why coroutines and what they are
- Standard ways to execute coroutines
- What dispatchers do and when they're needed
- How to make coroutines from blocking code and callbacks
- What is structured concurrency
- How to handle exceptions
- How to support cancellation

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
performing I/O operations. However, thanks to the native support in the Kotlin compiler for `suspend` functions, this
code is actually asynchronous and doesn't block the thread it's called from.

Another benefit of coroutines is that they adhere to a paradigm called _structured concurrency_. In the structured
concurrency world, asynchronous computations always live within a parent which delimits the computations' lifetime.
Following this paradigm makes it more difficult to leak computations, meaning that it makes it easier to avoid having
code running longer than its intended lifetime. In the Kotlin world, the parent is called _scope_
and is provided in the standard library as `CoroutineScope`.

## What is a coroutine

From the [doc](https://kotlinlang.org/docs/coroutines-basics.html):
> A coroutine is an instance of suspendable computation.

Coroutines are often promoted as lightweight threads and, from a conceptual standpoint, the comparison holds. Both are
instances of concurrent computations that, at any given point in time, can be running or paused. However, while a thread
is an actual runtime entity in the JVM linked to an operating system thread, a coroutine is a compile-time construct
that doesn't exist concretely at runtime.

Since programming is not magic (even though sometimes it looks like it), the code of a coroutine still needs a thread to
run, just like any other piece of code. At any point in time, a running coroutine is executing on a thread but it's not
tied to it. After suspension, it can be resumed on a different thread so the same coroutine can run on different threads
during its lifetime.
