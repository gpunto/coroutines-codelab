package dev.gianmarcodavid.coroutinesworkshop

import javax.inject.Inject

class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi,
    private val locationApi: LocationApi,
) {
    suspend fun getCurrentWeather(): Weather {
        val location = getCurrentLocation()
        val forecast = getForecast(location)
        return forecast.currentWeather
    }

    private suspend fun getCurrentLocation(): CurrentLocation {
        return locationApi.getCurrentLocation()
    }

    private suspend fun getForecast(location: CurrentLocation): Forecast {
        return weatherApi.getCurrentWeather(location.latitude, location.longitude)
    }
}
