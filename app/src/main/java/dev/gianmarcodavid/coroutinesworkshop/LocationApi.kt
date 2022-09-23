package dev.gianmarcodavid.coroutinesworkshop

import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface LocationApi {
    @GET("json")
    suspend fun getCurrentLocation(): CurrentLocation
}

@Serializable
data class CurrentLocation(
    val latitude: Double,
    val longitude: Double
)
