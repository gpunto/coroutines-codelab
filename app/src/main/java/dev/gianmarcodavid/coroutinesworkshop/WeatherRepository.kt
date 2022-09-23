package dev.gianmarcodavid.coroutinesworkshop

import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject

class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi,
    private val locationApi: LocationApi
) {
    fun getCurrentWeather(): Weather {
        val location = getCurrentLocation()
        val forecast = getForecast(location)
        return forecast.currentWeather
    }

    private fun getCurrentLocation(): CurrentLocation =
        locationApi.getCurrentLocation().execute().bodyOrThrow()

    private fun getForecast(location: CurrentLocation): Forecast =
        weatherApi.getCurrentWeather(location.latitude, location.longitude).execute()
            .bodyOrThrow()
}

private fun <T> Response<T>.bodyOrThrow(): T =
    if (isSuccessful) checkNotNull(body())
    else throw HttpException(this)
