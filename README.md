# 3. Making coroutines from callbacks

In this exercise, we will learn how to make coroutines from callbacks.

The code is similar to the one we started from in the previous exercise, with the difference that now the network
requests are executed asynchronously and return their responses through callbacks. Their signatures look like this:

```kotlin
fun getCurrentLocation(
    onSuccess: (CurrentLocation) -> Unit,
    onError: (Throwable) -> Unit
)

fun getForecast(
    location: CurrentLocation,
    onSuccess: (Forecast) -> Unit,
    onError: (Throwable) -> Unit
)
```

The use of callbacks in these functions trickles down to consumers, which have to use callbacks too. As a result,
`getCurrentWeather` in `WeatherRepository` starts to descend into callbacks hell and becomes much more complex to
understand:

```kotlin
fun getCurrentWeather(onSuccess: (Weather) -> Unit, onError: (Throwable) -> Unit) {
    getCurrentLocation(onSuccess = { location ->
        getForecast(location,
            onSuccess = { forecast ->
                thread {
                    weatherStorage.store(forecast.currentWeather)
                    onSuccess(forecast.currentWeather)
                }
            }, onError = {
                onError(it)
            })
    }, onError = {
        onError(it)
    })
}
```

Our task is to build coroutines out of these callbacks, so we can avoid the nesting and make the code look like it would
if it was blocking, similar to how it was in the previous exercise.

Both functions performing network requests are using internally the following custom `Call::fetch` function which, in
turn, delegates to Retrofit's default callback mechanism.

```kotlin
private fun <T> Call<T>.fetch(onSuccess: (T) -> Unit, onError: (Throwable) -> Unit) {
    this.enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (response.isSuccessful) {
                onSuccess(checkNotNull(response.body()))
            } else {
                onError(HttpException(response))
            }
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            onError(t)
        }
    })
}

```

This code should look familiar if you ever used naked Retrofit `Call`s. We enqueue the call and pass a callback that
will be notified in case we get a response or an error. We then handle each case and propagate the result to `onSuccess`
and `onError` as appropriate.

## Using `suspendCoroutine`

So how do we convert our callbacks to coroutines? Luckily for us, there's a function made exactly for that:

```kotlin
suspend inline fun <T> suspendCoroutine(crossinline block: (Continuation<T>) -> Unit): T
```

As you can imagine judging by its name, this function suspends the coroutine from where it's called. The interesting
part is the `block` lambda it receives and, in particular, its `Continuation` parameter.

```kotlin
interface Continuation<in T> {
    val context: CoroutineContext

    fun resumeWith(result: Result<T>)
}
```

A `Continuation` represents a handle to resume the associated coroutine. Its type parameter is the same as the
coroutine's return. If we look at the `resumeWith` function, we see that we need to pass a `Result`, meaning that we can
resume the coroutine with a value of type `T` or an exception. There are also two convenient extensions we can use
instead of directly passing a `Result`: `resume` and `resumeWithException`.

This is all we need to convert a callback: we wrap it into a `suspendCoroutine` call and we use the `Continuation` to
resume the coroutine whenever the callback is invoked. Let's try doing so in our `Call::fetch`:

```kotlin
// 1. Add suspend modifier
// 2. Remove onSuccess + onError callbacks and use T as return type
private suspend fun <T> Call<T>.fetch(): T {
    // 3. Wrap call to enqueue in suspendCoroutine
    return suspendCoroutine { continuation ->
        this.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (response.isSuccessful) {
                    // 4. Replace the onSuccess invocation with `continuation.resume`
                    continuation.resume(checkNotNull(response.body()))
                } else {
                    // 5. Replace the onError invocation with `continuation.resumeWithException
                    continuation.resumeWithException(HttpException(response))
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                // 5. Replace the onError invocation with `continuation.resumeWithException
                continuation.resumeWithException(t)
            }
        })
    }
}
```

We:

1. Added the `suspend` modifier, because `suspendCoroutine` is a `suspend` function
2. Removed the `onSuccess` and `onError` callbacks we were receiving as parameters because we don't need them anymore
3. Wrapped the call to `enqueue` in `suspendCoroutine`
4. Replaced `onSuccess` invocations with `continuation.resume`  
   (We could've also used `continuation.resumeWith(Result.success(value))`)
5. Replaced `onError` invocations with `continuation.resumeWithException`  
   (We could've also used `continuation.resumeWith(Result.failure(exception))`)

After these changes, we can easily convert `getCurrentLocation` and `getForecast` to suspending functions, by
adding `suspend` and removing the callbacks:

```kotlin
private suspend fun getCurrentLocation(): CurrentLocation {
    return locationApi.getCurrentLocation().fetch()
}

private suspend fun getForecast(location: CurrentLocation): Forecast {
    return weatherApi.getCurrentWeather(location.latitude, location.longitude).fetch()
}
```

Now, we can finally make `getCurrentWeather` simple again:

```kotlin
suspend fun getCurrentWeather(): Weather {
    val location = getCurrentLocation()
    val weather = getForecast(location).currentWeather
    withContext(Dispatchers.IO) {
        weatherStorage.store(weather)
    }
    return weather
}
```

It now looks surprisingly similar to the blocking function we had in the previous exercise. Note that
`weatherStorage.store` is still a blocking I/O call, so we change the dispatcher applying what we already learned.

The last bit to change is in `MainViewModel`, which can now ditch the callbacks and go back to launching the coroutine:

```kotlin
fun onButtonClick() {
    _uiState.value = UiState.Loading

    MainScope().launch {
        try {
            val weather = repository.getCurrentWeather()
            _uiState.postValue(UiState.Content(weather))
        } catch (e: Exception) {
            _uiState.postValue(UiState.Error(makeErrorMessage(e)))
        }
    }
}

```

Let's run the app and breathe in relief because everything should still work as we want.

## Forwarding cancellation

In our solution, we've used `suspendCoroutine` which doesn't allow us to react to cancellation. It means that if the
coroutine is canceled while the request is in progress, we won't notify Retrofit, so it will continue executing. What we
should do instead, is use `suspendCancellableCoroutine`:

```kotlin
suspend inline fun <T> suspendCancellableCoroutine(
    crossinline block: (CancellableContinuation<T>) -> Unit
): T
```

Its definition is very similar to `suspendCoroutine`, with the difference that we get a `CancellableContinuation` in the
lambda. Its API is much richer than `Continuation`'s one and, among the other functionalities it offers, it allows us to
register a cancellation callback through the `invokeOnCancellation` function. That's what we're going to use:

```kotlin
private suspend fun <T> Call<T>.fetch(): T {
    return suspendCancellableCoroutine { continuation -> // Replace suspendCoroutine with suspendCancellableCoroutine
        continuation.invokeOnCancellation { this.cancel() } // Cancel the Retrofit call when the coroutine is cancelled

        this.enqueue(object : Callback<T> {
            …
```

With this simple change we now forward the coroutine's cancellation to the Retrofit call, so that it can be properly
disposed and we don't waste resources.

## Bonus: Retrofit's default support for coroutines

Up until now, we've been using Retrofit's `Call` directly, but we don't need to! Retrofit natively supports `suspend`
functions, and the [logic][1] it uses is very similar to what we implemented above. So we can just add the modifier to
our API definitions and return the response type we expect instead of `Call`. For example, `LocationApi` becomes:

[1]: https://github.com/square/retrofit/blob/1490e6b1f90c616e2283b098a2c8e51a88cc97c2/retrofit/src/main/java/retrofit2/KotlinExtensions.kt

```kotlin
interface LocationApi {
    @GET("json")
    suspend fun getCurrentLocation(): CurrentLocation
}
```
