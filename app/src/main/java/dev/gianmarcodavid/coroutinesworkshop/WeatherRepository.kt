package dev.gianmarcodavid.coroutinesworkshop

import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject
import kotlin.concurrent.thread

class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi,
    private val locationApi: LocationApi,
    private val weatherStorage: WeatherStorage
) {
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

    private fun getCurrentLocation(onSuccess: (CurrentLocation) -> Unit, onError: (Throwable) -> Unit) {
        locationApi.getCurrentLocation().fetch(onSuccess, onError)
    }

    private fun getForecast(location: CurrentLocation, onSuccess: (Forecast) -> Unit, onError: (Throwable) -> Unit) {
        weatherApi.getCurrentWeather(location.latitude, location.longitude)
            .fetch(onSuccess, onError)
    }
}

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
