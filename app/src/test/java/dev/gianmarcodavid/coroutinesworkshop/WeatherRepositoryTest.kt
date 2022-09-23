package dev.gianmarcodavid.coroutinesworkshop

import dev.gianmarcodavid.coroutinesworkshop.doubles.FakeWeatherStorage
import dev.gianmarcodavid.coroutinesworkshop.doubles.StubLocationApi
import dev.gianmarcodavid.coroutinesworkshop.doubles.StubWeatherApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class WeatherRepositoryTest {

    private val stubWeather = Weather(temperature = 10.5, windSpeed = 20.0)
    private val weatherApi: WeatherApi = StubWeatherApi(stubWeather)
    private val locationApi: LocationApi = StubLocationApi()
    private val weatherStorage: WeatherStorage = FakeWeatherStorage()

    private val repository = WeatherRepository(weatherApi, locationApi, weatherStorage)

    @Test
    fun `when getCurrentWeather, then fetch, store, and return the weather`() {
        val result = repository.getCurrentWeather()

        assertEquals(stubWeather, result)
        assertEquals(stubWeather, weatherStorage.get())
    }
}
