package dev.gianmarcodavid.coroutinesworkshop

import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.http.GET

interface LocationApi {
    @GET("json")
    fun getCurrentLocation(): Call<CurrentLocation>
}

@Serializable
data class CurrentLocation(
    val latitude: Double,
    val longitude: Double
)
