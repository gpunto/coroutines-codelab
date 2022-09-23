package dev.gianmarcodavid.coroutinesworkshop

import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject

class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi,
    private val locationApi: LocationApi,
    private val weatherStorage: WeatherStorage
) {
    fun getCurrentWeather(): Weather {
        val location = getCurrentLocation()
        val weather = getForecast(location).currentWeather
        weatherStorage.store(weather)
        return weather
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
