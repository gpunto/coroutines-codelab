package dev.gianmarcodavid.coroutinesworkshop.doubles

import dev.gianmarcodavid.coroutinesworkshop.Forecast
import dev.gianmarcodavid.coroutinesworkshop.Weather
import dev.gianmarcodavid.coroutinesworkshop.WeatherApi

class StubWeatherApi(private val stubWeather: Weather) : WeatherApi {
    override suspend fun getCurrentWeather(latitude: Double, longitude: Double) = Forecast(stubWeather)
}
