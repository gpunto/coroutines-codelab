# 4. Structured concurrency

In this exercise, we will learn how structured concurrency helps us limit the lifetime of a coroutine.

The code we will be using is very similar to what we had with a few differences:

1\. `MainActivity` displays an additional button to call `finish()` on tap. We also register a lifecycle observer to log
the state changes:

```kotlin
binding.cancelButton.setOnClickListener {
    finish()
}
lifecycle.addObserver(LifecycleEventObserver { _, event ->
    Log.i("MainActivity", "Current lifecycle state: ${event.name}")
})
```

2\. `MainViewModel` gains some log statements so we can see when things happen:

```kotlin
fun onButtonClick() {
    _uiState.value = UiState.Loading

    Log.i("MainViewModel", "Launching coroutine")
    MainScope().launch {
        try {
            val weather = repository.getCurrentWeather()
            Log.i("MainViewModel", "Got weather")
            _uiState.postValue(UiState.Content(weather))
        } catch (e: Exception) {
            _uiState.postValue(UiState.Error(makeErrorMessage(e)))
        }
    }
}

override fun onCleared() {
    super.onCleared()
    Log.i("MainViewModel", "onCleared")
}
```

3\. `WeatherRepository::getCurrentWeather` becomes slower thanks to an artificial `delay`:

```kotlin
suspend fun getCurrentWeather(): Weather {
    delay(5000)
    val location = getCurrentLocation()
    …
```

With all of this in mind, let's run the app, tap on the button to get the weather and the finish button quickly after.
If we check the Logcat after doing so, we should see a timeline resembling this one:

```
I/MainViewModel: Launching coroutine
I/MainActivity: Current lifecycle state: ON_PAUSE
I/MainActivity: Current lifecycle state: ON_STOP
I/MainActivity: Current lifecycle state: ON_DESTROY
I/MainViewModel: onCleared
[…] ~5 seconds delay
[…] the two API calls
I/MainViewModel: Got weather
```

That doesn't seem right. The Activity has been destroyed, the ViewModel's `onCleared` has been called, but the coroutine
continues running until completion. This bug shouldn't surprise us, though, because our code doesn't contain any logic
about cancellation. Let's fix that!

In the intro to this workshop we said that coroutines adhere to a paradigm called _structured concurrency_, meaning that
each of them has a parent delimiting its children's lifetime. This parent is the `CoroutineScope` and its `cancel`
extension function is the way to stop the children coroutines:

```kotlin
fun CoroutineScope.cancel(cause: CancellationException? = null)
```

In `MainViewModel`, we get an instance of `CoroutineScope` by invoking the `MainScope` function. So what we need to do
is call `cancel` on this instance at the right time, which, in our case, is in the ViewModel's `onCleared`:

```kotlin
private val scope = MainScope() // 1. Extract the scope to a class property

fun onButtonClick() {
    _uiState.value = UiState.Loading

    Log.i("MainViewModel", "Launching coroutine")
    scope.launch { // 2. Use the scope property instead of creating a new one
        …
    }
}

override fun onCleared() {
    super.onCleared()
    Log.i("MainViewModel", "onCleared")
    scope.cancel() // 3. Cancel the scope
}
```

If we now run the app and test the same scenario as before (tap on "Get current weather", tap on "Finish"), we will see
that our logs stop with the ViewModel's `onCleared`, and this means that the coroutine is properly canceled:

> I/MainViewModel: Launching coroutine  
> I/MainActivity: Current lifecycle state: ON_PAUSE  
> I/MainActivity: Current lifecycle state: ON_STOP  
> I/MainActivity: Current lifecycle state: ON_DESTROY  
> I/MainViewModel: onCleared

## Ready-made scopes

Up until now, the burden of managing the scope was on ourselves: we used the `MainScope` function to instantiate it and
then we had to cancel it manually. However, this isn't what we would normally do in an Android app, because androidx
libraries provide ready-made scopes that are canceled automatically at the right time. For example:

- In Activity or Fragment: `lifecycleScope`, canceled in `onDestroy`
- In a `@Composable`: `rememberCoroutineScope()`, canceled when it leaves the composition
- In a ViewModel: `viewModelScope`, canceled in `onCleared`

So let's replace our custom logic with a call to the latter and ditch our `scope` property entirely:

```kotlin
// 1. Delete scope property
// private val scope = MainScope()

fun onButtonClick() {
    _uiState.value = UiState.Loading

    Log.i("MainViewModel", "Launching coroutine")
    viewModelScope.launch { // 2. Use viewModelScope instead of the scope property
        …
    }
}

override fun onCleared() {
    super.onCleared()
    Log.i("MainViewModel", "onCleared")
    // 3. Remove call to cancel
    // scope.cancel()
}
```

Let's now run the app again and convince ourselves that it's working as before.
