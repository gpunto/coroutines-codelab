# 5. Handling exceptions

In this exercise, we will learn how to handle exceptions in a coroutine and avoid a common pitfall.

The setup is similar to the previous exercise: there's still a second button, but this time it notifies `MainViewModel`
by calling `onCancelClick` instead of finishing the Activity:

```kotlin
binding.cancelButton.setOnClickListener {
    viewModel.onCancelClick()
}
```

The purpose of this function is to cancel the current coroutine, if any, which is done by calling `cancel` on the `Job`
associated to it. We encountered the `Job` interface earlier in exercise 1, where we learned that it's returned by
`launch` and that it can be used for cancellation, as we're doing here.

So, the starting implementation of `MainViewModel` now contains a `job` property which gets assigned when launching the
coroutine and then used in `onCancelClick`.

```kotlin
private var job: Job? = null

fun onButtonClick() {
    _uiState.value = UiState.Loading

    Log.i("MainViewModel", "Launching coroutine")
    job = viewModelScope.launch {
        try {
            …
        }
        Log.i("MainViewModel", "The coroutine is still alive")
    }
}

fun onCancelClick() {
    Log.i("MainViewModel", "Cancelling job $job")
    job?.cancel()
}
```

The last bit of news is the log statement at the end of the coroutine, outside of the try-catch. We've been using this
try-catch up until now and it served us well. If we ever got an exception, it was caught and handled as if we were using
regular non-suspending functions. However, **it's also been silently hiding a bug from us**. Let's reproduce it.

Let's run the app, tap on the usual "Get current weather" button and then on "Cancel". If we check the logs, we should
see something like:

> I/MainViewModel: Launching coroutine  
> I/MainViewModel: Cancelling job StandaloneCoroutine{Active}@d997ce3  
> I/MainViewModel: The coroutine is still alive

It seems like the cancellation is not working as it should. While "Got weather" is never printed (provided that we
canceled soon enough), we still get the log after the try-catch, indicating that the coroutine was still running. This
happens because our catch clause is too greedy and it's catching something it shouldn't: a `CancellationException`.

Kotlin coroutines use `CancellationException` to propagate _normal_ cancellation and we are interfering with that
mechanism. There are several ways to fix our code, let's see some.

## Catch only the exception(s) we care about

The simplest solution, which should be the default when we catch exceptions, is to be specific in our catch clause:

```kotlin
try {
    …
} catch (e: HttpException) { // Catching only HttpException
    _uiState.postValue(UiState.Error(makeErrorMessage(e)))
}
```

## Check for `CancellationException` and rethrow it

In some cases, we don't know what exceptions the code can throw. In these situations, an alternative solution is to
rethrow `CancellationException` when we get one:

```kotlin
try {
    …
} catch (e: Exception) {
    if (e is CancellationException) throw e // Check and rethrow
    _uiState.postValue(UiState.Error(makeErrorMessage(e)))
}
Log.i("MainViewModel", "The coroutine is still alive")
```

Note that this is also true for `runCatching` and similar helpers:

```kotlin
runCatching {
    repository.getCurrentWeather()
}.onFailure {
    if (it is CancellationException) throw it
}

```

## Use a `CoroutineExceptionHandler` instead of try-catch

The coroutines library offers a standard way to handle exceptions: `CoroutineExceptionHandler`.

```kotlin
// Create the handler containing the logic to process exceptions
private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
    _uiState.postValue(UiState.Error(makeErrorMessage(throwable)))
}

fun onButtonClick() {
    _uiState.value = UiState.Loading

    Log.i("MainViewModel", "Launching coroutine")
    job = viewModelScope.launch(exceptionHandler) { // Pass the handler
        // Remove the try-catch
        val weather = repository.getCurrentWeather()
        Log.i("MainViewModel", "Got weather")
        _uiState.postValue(UiState.Content(weather))
        Log.i("MainViewModel", "The coroutine is still alive")
    }
}
```

The handler is called only when an unexpected and uncaught exception occurs and will ignore `CancellationException`,
which is the behavior we want. It also allows us to decouple the happy path from the error handling, which may be
convenient in some cases.

Like `CoroutineDispatcher` we saw in exercise 2, `CoroutineExceptionHandler` is an implementation of `CoroutineContext`
and is another configuration we can set on a coroutine. In the code above, we are passing it to `launch`, but we may use
it wherever a `CoroutineContext` is expected.

If we test the app again we should now see that the coroutine is properly cancelled and the last log statement is never
executed.

Here's the [full solution](../../tree/05-solution) if you want to check it. Otherwise, **let's move to the
[next exercise](../../tree/06-cooperative_cancellation).**
