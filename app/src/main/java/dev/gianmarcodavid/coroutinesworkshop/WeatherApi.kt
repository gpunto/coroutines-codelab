package dev.gianmarcodavid.coroutinesworkshop

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("forecast?&current_weather=true&timezone=Europe%2FBerlin")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): Forecast
}

@Serializable
data class Forecast(
    @SerialName("current_weather")
    val currentWeather: Weather
)

@Serializable
data class Weather(
    val temperature: Double,
    @SerialName("windspeed")
    val windSpeed: Double,
)

