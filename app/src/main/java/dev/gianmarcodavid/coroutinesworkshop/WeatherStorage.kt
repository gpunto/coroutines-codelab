package dev.gianmarcodavid.coroutinesworkshop

interface WeatherStorage {
    fun store(weather: Weather)
    fun get(): Weather?
}
