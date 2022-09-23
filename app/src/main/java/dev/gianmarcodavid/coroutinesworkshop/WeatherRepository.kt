package dev.gianmarcodavid.coroutinesworkshop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

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
        return locationApi.getCurrentLocation()
    }

    private suspend fun getForecast(location: CurrentLocation): Forecast {
        return weatherApi.getCurrentWeather(location.latitude, location.longitude)
    }
}
