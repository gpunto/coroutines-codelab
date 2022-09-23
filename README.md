# 6. Cooperative cancellation

In this exercise, we will learn what it means when we say that cancellation is cooperative in coroutines and how to
support it in our suspending functions.

The setup is similar to the previous one. The app shows the "Get current weather" and "Cancel" buttons and they work as
before. However, we now have a long task to execute before requesting the weather from the repository:

```kotlin
private suspend fun longTask() {
    withContext(Dispatchers.Default) {
        repeat(10) {
            Log.i("MainViewModel", "Executing step $it")
            Thread.sleep(500)
        }
    }
}
```

The purpose of this function is to emulate a long multi-step operation that blocks the thread it's called from. As we
learned in exercise 2, we set the dispatcher to make this code run in a background thread. We are using
`Dispatchers.Default`, which is meant for CPU-intensive work.

So, as mentioned above, we call `longTask` in `onButtonClick` before requesting the weather. We also log each step.

```kotlin
fun onButtonClick() {
    _uiState.value = UiState.Loading

    Log.i("MainViewModel", "Launching coroutine")
    job = viewModelScope.launch(exceptionHandler) {
        longTask()
        Log.i("MainViewModel", "After long task")
        val weather = repository.getCurrentWeather()
        Log.i("MainViewModel", "Got weather")
        _uiState.postValue(UiState.Content(weather))
        Log.i("MainViewModel", "End of coroutine")
    }
}
```

Let's run the app, get the weather and cancel immediately after, and have a look at the Logcat. We should see something
along the lines of:

> I/MainViewModel: Launching coroutine  
> I/MainViewModel: Executing step 0  
> I/MainViewModel: Executing step 1  
> I/MainViewModel: Executing step 2  
> I/MainViewModel: Executing step 3  
> I/MainViewModel: Cancelling job StandaloneCoroutine{Active}@d997ce3  
> I/MainViewModel: Executing step 4  
> I/MainViewModel: Executing step 5  
> I/MainViewModel: Executing step 6  
> I/MainViewModel: Executing step 7  
> I/MainViewModel: Executing step 8  
> I/MainViewModel: Executing step 9

Here we go again… the coroutine is not being cancelled. However, the issue seems to only affect `longTask`, because
nothing else is printed after it finishes. So what's happening?

Cancellation, in coroutines, is cooperative. It means that there is no way to _forcibly_ stop a running one. For a
coroutine to be canceled, it must check for cancellation and throw `CancellationException`. This behavior is implemented
by all suspending functions in the coroutines library. We can demonstrate that by adding a call do `delay` at the
beginning of each iteration in `longTask`:

```kotlin
private suspend fun longTask() {
    withContext(Dispatchers.Default) {
        repeat(10) {
            delay(1) // Delay with an argument > 0
            Log.i("MainViewModel", "Executing step $it")
            Thread.sleep(500)
        }
    }
}
```

If we run the app and perform the same test as before, we should now get something like:

> I/MainViewModel: Launching coroutine  
> I/MainViewModel: Executing step 0  
> I/MainViewModel: Executing step 1  
> I/MainViewModel: Executing step 2  
> I/MainViewModel: Cancelling job StandaloneCoroutine{Active}@d997ce3

This time, the computation is canceled because `delay` checks internally if the coroutine is active and throws
`CancellationException` if it's not.

Using a random suspending call like `delay` for cooperating with cancellation may be confusing because it doesn't
clearly signal what its real purpose is. Instead, we should use the functions provided for the job: `isActive`,
`ensureActive`, `yield`.

## Using `isActive`

`isActive` is an extension of `CoroutineScope` and part of `Job`'s API, and, as the name suggests, returns true when the
coroutine is still running. If we wanted to apply it to our code we could do:

```kotlin
private suspend fun longTask() {
    withContext(Dispatchers.Default) {
        repeat(10) {
            if (!isActive) return@repeat // return if the coroutine is not active
            Log.i("MainViewModel", "Executing step $it")
            Thread.sleep(500)
        }
    }
}
```

`isActive` gives us the freedom to execute arbitrary code when the coroutine is canceled but it also means we need to
write the cancellation logic ourselves.

## Using `ensureActive`

`ensureActive` is an extension of `CoroutineScope` and `Job` and implements the standard cancellation logic, i.e. it
throws `CancellationException` if the coroutine is not active. Let's use it instead of the manual check:

```kotlin
private suspend fun longTask() {
    withContext(Dispatchers.Default) {
        repeat(10) {
            ensureActive() // throw CancellationException if the coroutine is not active
            Log.i("MainViewModel", "Executing step $it")
            Thread.sleep(500)
        }
    }
}
```

## Using `yield`

`yield` calls `ensureActive` internally but it also yields the thread or thread pool belonging to the current dispatcher
so that it can be used for running other coroutines.

```kotlin
private suspend fun longTask() {
    withContext(Dispatchers.Default) {
        repeat(10) {
            yield() // ensure active and yield the thread
            Log.i("MainViewModel", "Executing step $it")
            Thread.sleep(500)
        }
    }
}
```

In our case, there shouldn't be any behavioral difference between `ensureActive` and `yield` since we don't have
multiple coroutines scheduled to run with the same dispatcher. However, `yield` is a good choice for computation tasks
because it prevents us from starving other coroutines. If we never yield and the number of coroutines exceeds the number
of available threads, the ones in excess will stay paused until some of those running finish.
