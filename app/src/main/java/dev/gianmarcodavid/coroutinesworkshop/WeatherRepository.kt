package dev.gianmarcodavid.coroutinesworkshop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi,
    private val locationApi: LocationApi,
    private val weatherStorage: WeatherStorage
) {
    suspend fun getCurrentWeather(): Weather {
        val location = getCurrentLocation()
        val weather = getForecast(location).currentWeather
        withContext(Dispatchers.IO) {
            weatherStorage.store(weather)
        }
        return weather
    }

    private suspend fun getCurrentLocation(): CurrentLocation {
        return locationApi.getCurrentLocation().fetch()
    }

    private suspend fun getForecast(location: CurrentLocation): Forecast {
        return weatherApi.getCurrentWeather(location.latitude, location.longitude).fetch()
    }
}

private suspend fun <T> Call<T>.fetch(): T {
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { this.cancel() }

        this.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (response.isSuccessful) {
                    continuation.resume(checkNotNull(response.body()))
                } else {
                    continuation.resumeWithException(HttpException(response))
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                continuation.resumeWithException(t)
            }
        })
    }
}
