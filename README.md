# 1. Executing coroutines

In this first exercise, we will explore the three standard ways of launching coroutines in production code:
`runBlocking`, `launch`, and `async`. These are also known as **coroutine builders**.

`MainActivity` contains all the code for the exercise. The UI contains a text, a loader, and a button. The Activity
reacts to button clicks as follows:

1. Show a loading state
2. Execute a couple of long tasks, represented by `suspend` functions
3. Display the tasks' results when they're finished

As we can see from the code, this is already implemented in the `onButtonClick` function. However, if we try to run the
app the compiler blocks us:

```
e: MainActivity.kt: (35, 23): Suspend function 'aLongTask' should be called only from a coroutine or another suspend function
e: MainActivity.kt: (36, 29): Suspend function 'anotherLongTask' should be called only from a coroutine or another suspend function
```

The code won't compile as it is, because `suspend` functions can only be called from suspending code.

## Using `runBlocking`

The first coroutine builder we're going to try is `runBlocking`. It has a signature that conveniently fits our
requirements:

```kotlin
fun <T> runBlocking(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T
): T
```

It is not a `suspend` function, so we can call it from anywhere, and its `block` parameter is a suspending lambda, so it
can run suspending code. Let's use it! Let's wrap `onButtonClick`'s body in `runBlocking`:

```kotlin
private fun onButtonClick() {
    runBlocking {
        setState(loading = true, text = "Starting a couple of long tasks")
        val aResult = aLongTask()
        val anotherResult = anotherLongTask()
        val combinedResult = "$aResult $anotherResult"
        setState(loading = false, text = "The tasks completed with: $combinedResult")
    }
}
```

The compiler is now happy so we can run the app. However, when we tap on the button the UI freezes while the tasks are
running and this means we are blocking the main thread. It happens because `runBlocking` **blocks the thread from which
it's called** until the `block` lambda and all coroutines launched from it complete.

`runBlocking`'s purpose is to bridge between regular blocking code and coroutines. It's useful when we need to call
suspending functions from code that is expected to be blocking, for example the Java `main` function, tests, or
background threads.

Since it doesn't fit our use case, let's see what we can do instead.

## Using `launch`

`launch` is probably the coroutine builder you're going to use the most. Its signature is:

```kotlin
fun CoroutineScope.launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job
```

Similarly to `runBlocking`, `launch` accepts a suspending lambda while not being a `suspend` function, but, differently
from `runBlocking`, it's defined as an extension of `CoroutineScope`, so we'll need one of those. For this exercise,
let's create one through the `MainScope` function and wrap `onButtonClick`'s body in `MainScope().launch`:

```kotlin
private fun onButtonClick() {
    MainScope().launch { // Wrap in launch
        setState(loading = true, text = "Starting a couple of long tasks")
        val aResult = aLongTask()
        val anotherResult = anotherLongTask()
        val combinedResult = "$aResult $anotherResult"
        setState(loading = false, text = "The tasks completed with: $combinedResult")
    }
}
```

We run the app, tap on the button, and finally everything seems to work: the tasks are executed and the UI is updated
without blocking the main thread. This is precisely `launch`'s purpose: launching a new coroutine without blocking the
current thread. This also means that **the coroutine will run asynchronously** with respect to the code outside of
`launch`. To convince ourselves this is the case, let's add some code outside of the lambda, for example:

```kotlin
private fun onButtonClick() {
    MainScope().launch {
        …
    }
    Log.i("MainActivity", "I'm placed after launch's lambda") // Add this outside outside of launch
}
```

If we run the app and tap on the button, we'll see that this log printed while the loading state is showing, proving
again that the coroutine is running asynchronously.

Now that we acquired the power to _launch_ code concurrently, we might want to optimize how `onButtonClick` behaves. The
two tasks are independent from each other, so why are we running them sequentially? Let's use `launch` to make them
asynchronous:

```kotlin
private fun onButtonClick() {
    MainScope().launch {
        setState(loading = true, text = "Starting a couple of long tasks")
        val aResult = launch { aLongTask() } // Wrap in launch here…
        val anotherResult = launch { anotherLongTask() } // …and here
        val combinedResult = "$aResult $anotherResult"
        setState(loading = false, text = "The tasks completed with: $combinedResult")
    }
}
```

Note that the for the inner `launch` calls we aren't specifying the `CoroutineScope`. Why? Do you remember the last
parameter of `launch`'s signature? It's a lambda with `CoroutineScope` as receiver, so we are using the scope bound by
the outer `launch`.

```kotlin
fun CoroutineScope.launch(
    …
    block: suspend CoroutineScope.() -> Unit // CoroutineScope is the receiver 
): Job
```

Enough talk. Let's run the app, tap on the button, and… something's off. The text displays a string similar to:

> The tasks completed with StandaloneCoroutine{Active}@a0b67b1 StandaloneCoroutine{Active}@4594496

If we check again `launch`'s signature, we can see that it returns a `Job`. A `Job` is an interface representing the
coroutine's lifecycle and can be used for cancellation. As it is, it can't be used to extract the coroutine's result, so
the data our tasks return is lost. Here we're displaying the output of the `toString` function for the `Job`s. Also, the
coroutines started with `launch` are asynchronous, so we aren't even waiting for the results to be returned.

Well, `launch` can't help us much more, let's see what we can do instead.

## Using `async`

Meet `async`, the third and final standard coroutine builder:

```kotlin
fun <T> CoroutineScope.async(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T>
```

This signature is very similar to `launch`'s, with the return type as key difference: `Deferred<T>` instead of `Job`.
Actually, "instead" might be misleading, because:

```kotlin
interface Deferred<out T> : Job
```

`Deferred` is _also_ a `Job`! In addition, **we can use it to extract a result from a coroutine**, normally by
calling `await`. Conceptually, `Deferred` is very similar to Java's `Future`. It represents the result of an
asynchronous computation and offers a function to wait for the result. But while `future.get()` blocks the thread,
`deferred.await()` suspends the coroutine.

Let's use the `async` + `await` combo to fix our code:

```kotlin
private fun onButtonClick() {
    MainScope().launch {
        setState(loading = true, text = "Starting a couple of long tasks")
        val aResult = async { aLongTask() } // Replace launch with async here…
        val anotherResult = async { anotherLongTask() } // …and here…
        val combinedResult = "${aResult.await()} ${anotherResult.await()}" // …and call await on both Deferred 
        setState(loading = false, text = "The tasks completed with: $combinedResult")
    }
}
```

You already know the drill by now: launch the app, tap on the button and… it works! The tasks are run concurrently and
we are able to pause and wait for the results to be ready.

Here's the [full solution](../../tree/01-solution) if you want to check it. Otherwise, **let's move to the
[next exercise](../../tree/02-blocking2coroutines).**
