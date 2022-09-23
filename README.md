# 2. Making coroutines from blocking code

In this exercise, we will learn how to create coroutines from blocking code.

From now on, we are going to work on an application to show the weather. Try to run it and see how it behaves. You
should be presented with a button to get the current weather. After tapping on it, you should see the loading state
first, and the weather after that. If any error occurs, it will be displayed on the UI.

Code-wise, the app's structure should be familiar:

- The data comes from two APIs: `LocationApi`, to get the current location, and `WeatherApi`, to get the weather for the
  location
- `WeatherStorage`: used to cache the latest weather fetched
- `WeatherRepository`: in charge of the logic to fetch, cache, and return the current weather
- `MainViewModel`: retrieves the weather through the repository and exposes the state for the UI
- `MainActivity`: displays the state exposed by the ViewModel

If you open `WeatherRepository`, you'll see the logic is straightforward and easy to follow:

```kotlin
fun getCurrentWeather(): Weather {
    val location = getCurrentLocation()
    val weather = getForecast(location).currentWeather
    weatherStorage.store(weather)
    return weather
}
```

However, this function blocks the thread it's called from for a considerable amount of time, considering that it
performs three I/O operations: two network calls (`getCurrentLocation`, `getForecast`), and a write to the local
storage (`WeatherStorage::store`). Indeed, if we were to call it from the main thread, the app would crash with a
`NetworkOnMainThreadException` since Android forbids network operations on the main thread. This is why `MainViewModel`
spawns a thread:

```kotlin
fun onButtonClick() {
    _uiState.value = UiState.Loading

    thread {
        try {
            val weather = repository.getCurrentWeather()
            _uiState.postValue(UiState.Content(weather))
        } catch (e: Exception) {
            _uiState.postValue(UiState.Error(makeErrorMessage(e)))
        }
    }
}
```

This code too is quite easy to follow. We get the current weather from the repository and we notify the UI at each step
along the way. Also, a try-catch is all we need for handling errors.

Our job is to convert this code to coroutines, so let's apply what we learned in the previous exercise and use `launch`
instead of creating a thread:

```kotlin
fun onButtonClick() {
    _uiState.value = UiState.Loading

    MainScope().launch { // Replace thread with MainScope().launch
        try {
            val weather = repository.getCurrentWeather()
            _uiState.postValue(UiState.Content(weather))
        } catch (e: Exception) {
            _uiState.postValue(UiState.Error(makeErrorMessage(e)))
        }
    }
}
```

We run the app, tap on the button, and…

> Got an exception: NetworkOnMainThreadException

Let's see what's happening here. In the previous chapters, we said that `launch` executes a new coroutine
asynchronously, but we also said that the code for every coroutine must be executed on some thread. So which thread is
executing our code? To get to the answer, we need to know that every coroutine has an associated `CoroutineContext`
which, in our case, is inherited from the `CoroutineScope`:

```kotlin
interface CoroutineScope {
    val coroutineContext: CoroutineContext
}
```

We can think of `CoroutineContext` as a bag of elements where every element is itself a `CoroutineContext`. A context
represents a set of configurations for a coroutine. One of the things we can configure is the `CoroutineDispatcher`,
which is the way to specify on which threads the coroutine can be run. Common dispatchers can be accessed through
the `Dispatchers` object. If we check what the `MainScope` function we're calling does, we see that it's using
`Dispatchers.Main`, which, as you might guess from the name, on Android uses the main thread.

```kotlin
fun MainScope(): CoroutineScope = ContextScope(SupervisorJob() + Dispatchers.Main)
```

All of this means that, in our ViewModel, we do launch the coroutine asynchronously, but all of the _blocking_ code it
contains runs on the main thread. This is true for every non-suspending line of code.

What we have have to do, then, is use a different dispatcher, in particular `Dispatchers.IO`, which is designed for I/O
operations. We have several ways to set it:

We can override it for the whole scope, so that all coroutines started from it will be executed in background:

```kotlin
fun onButtonClick() {
    _uiState.value = UiState.Loading

    val scope = MainScope() + Dispatchers.IO // We need to import kotlinx.coroutines.plus
    scope.launch {
        …
```

We can set it for a specific portion of the code, by wrapping it in `withContext`:

```kotlin
MainScope().launch {
    withContext(Dispatchers.IO) {
        try {
            val weather = repository.getCurrentWeather()
            _uiState.postValue(UiState.Content(weather))
        } catch (e: Exception) {
            _uiState.postValue(UiState.Error(makeErrorMessage(e)))
        }
    }
}

```

Or, we can set it for the whole coroutine by passing it as `launch`'s first argument:

```kotlin
fun onButtonClick() {
    _uiState.value = UiState.Loading

    MainScope().launch(Dispatchers.IO) {
        …
```

If we now run the app, everything should be working again since the content of the lambda is executed on a background
thread.
