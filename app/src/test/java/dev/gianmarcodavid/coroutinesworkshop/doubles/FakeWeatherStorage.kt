package dev.gianmarcodavid.coroutinesworkshop.doubles

import dev.gianmarcodavid.coroutinesworkshop.Weather
import dev.gianmarcodavid.coroutinesworkshop.WeatherStorage

class FakeWeatherStorage : WeatherStorage {
    private var stored: Weather? = null

    override fun store(weather: Weather) {
        stored = weather
    }

    override fun get(): Weather? = stored
}
