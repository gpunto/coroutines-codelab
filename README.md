# 1. Executing coroutines

In this first exercise, we will explore the three standard ways of launching coroutines in production code:
`runBlocking`, `launch`, and `async`. These are also known as coroutine builders.

`MainActivity` contains all the code for the exercise. The UI contains a text, a loader, and a button. The Activity's
job is to react to button clicks by launching a couple of long tasks, showing the loading state while they're executing,
and displaying their results when they're finished. As you can see, this is already implemented: the two tasks are
represented by `suspend` functions and the logic for calling them and updating the UI is already in place
in `onButtonClick`. However, if we try to run the app the compiler blocks us:

```
e: MainActivity.kt: (35, 23): Suspend function 'aLongTask' should be called only from a coroutine or another suspend function
e: MainActivity.kt: (36, 29): Suspend function 'anotherLongTask' should be called only from a coroutine or another suspend function
```

The code won't compile as it is, because `suspend` functions can only be called from suspending code and this rule is
enforced at compile time.

## Using `runBlocking`

The first coroutine builder we're going to try is `runBlocking`, which has a signature that conveniently fits our
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
        showLoading(true)
        displayText("Starting a couple of long tasks")
        val aResult = aLongTask()
        val anotherResult = anotherLongTask()
        val combinedResult = "$aResult $anotherResult"
        showLoading(false)
        displayText("The tasks completed with: $combinedResult")
    }
}
```

The compiler is now happy so we can run the app. However, when we click the button the UI freezes while the tasks are
running and this means we are blocking the main thread. It happens because, as the name suggests, `runBlocking` blocks
the thread from which it's called until the `block` lambda and all coroutines launched from it complete. The purpose of
this function is to act as a bridge between regular blocking code and coroutines. It's useful when you need to call
suspending functions from code that is expected to be blocking, for example the Java `main` function, tests, or
background threads.

It's now clear that `runBlocking` doesn't fit our use case, so let's see what we can do instead.

## Using `launch`

`launch` is probably the coroutine builder you're going to encounter the most. Its signature is:

```kotlin
fun CoroutineScope.launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job
```

Similarly to `runBlocking`, `launch` accepts a suspending lambda while not being a `suspend` function, but, differently
from `runBlocking`, it's defined as an extension of `CoroutineScope`, so we'll need one of those to call it. For this
exercise, let's create a scope through the `MainScope` function and wrap `onButtonClick`'s body in `MainScope().launch`:

```kotlin
private fun onButtonClick() {
    MainScope().launch {
        showLoading(true)
        displayText("Starting a couple of long tasks")
        val aResult = aLongTask()
        val anotherResult = anotherLongTask()
        val combinedResult = "$aResult $anotherResult"
        showLoading(false)
        displayText("The tasks completed with: $combinedResult")
    }
}
```

We run the app, tap on the button, and finally everything seems to work: the tasks are executed and the UI is updated
without blocking the main thread. This is precisely `launch`'s purpose: launching a new coroutine without blocking the
current thread. This also means that the coroutine will run asynchronously with respect to the code outside of `launch`.
To convince ourselves this is the case, let's add some code outside of the lambda, for example:

```kotlin
private fun onButtonClick() {
    MainScope().launch {
        …
    }
    Log.i("MainActivity", "I'm placed after launch's lambda") // Add this
}
```

If we run the app and tap on the button, we'll see that this log is printed while the loading state is showing, proving
again that the coroutine is running asynchronously.

Now that we acquired the power to _launch_ code concurrently, we might want to optimize how `onButtonClick` behaves. The
two tasks we execute are independent from each other, so why are we running them sequentially? Let's use `launch` to
make them asynchronous:

```kotlin
private fun onButtonClick() {
    MainScope().launch {
        showLoading(true)
        displayText("Starting a couple of long tasks")
        val aResult = launch { aLongTask() } // Wrap in launch here…
        val anotherResult = launch { anotherLongTask() } // …and here
        val combinedResult = "$aResult $anotherResult"
        showLoading(false)
        displayText("The tasks completed with: $combinedResult")
    }
}
```

You might be wondering why we have to specify the `CoroutineScope` for the outer `launch` while we aren't doing the same
for the inner ones. It's simple: do you remember the last parameter of `launch`'s signature? It's a lambda with
receiver. It binds `CoroutineScope` to `this`, so we are using the scope bound by the outer `launch`.

```kotlin
fun CoroutineScope.launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit // CoroutineScope is the receiver 
): Job
```

Enough talk. Let's run the app, tap on the button, and… something's off. The text displays a string similar to:

> The tasks completed with StandaloneCoroutine{Active}@a0b67b1 StandaloneCoroutine{Active}@4594496

If we check again `launch`'s signature, we can see that it returns a `Job`. A `Job` is an interface representing the
coroutine's lifecycle and can be used for cancellation. As it is, it can't be used to extract the coroutine's result, so
the data our tasks return is lost. What we're displaying here is the output of the `toString` function for the `Job`s.
Also, the coroutines started with `launch` are asynchronous, so we aren't even waiting for the results to be returned.

Well, `launch` can't help us much more, so let's see what we can do instead.

## Using `async`

Meet `async`, the third and final standard coroutine builder:

```kotlin
fun <T> CoroutineScope.async(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T>
```

This signature is very similar to `launch`'s, with the key difference that it returns `Deferred<T>` instead of `Job`.
Actually, "instead" might be misleading, because:

```kotlin
interface Deferred<out T> : Job
```

`Deferred` is _also_ a `Job`! In addition to that, we can use it to extract a result from a coroutine, normally by
calling `await` on it. Conceptually, `Deferred` is very similar to Java's `Future`. It represents the result of an
asynchronous computation and offers a function that allows us to wait for the result. But while calling `future.get()`
blocks the thread, calling `deferred.await()` only suspends the coroutine.

Let's use the `async` + `await` combo to fix our code:

```kotlin
private fun onButtonClick() {
    MainScope().launch {
        showLoading(true)
        displayText("Starting a couple of long tasks")
        val aResult = async { aLongTask() } // Replace launch with async here…
        val anotherResult = async { anotherLongTask() } // …and here…
        val combinedResult = "${aResult.await()} ${anotherResult.await()}" // …and call await on both Deferred 
        showLoading(false)
        displayText("The tasks completed with: $combinedResult")
    }
}
```

You already know the drill by now: launch the app, tap on the button and… it works! The tasks are run concurrently and
we are able to pause and wait for the results to be ready. 
