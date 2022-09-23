package dev.gianmarcodavid.coroutinesworkshop.doubles

import dev.gianmarcodavid.coroutinesworkshop.CurrentLocation
import dev.gianmarcodavid.coroutinesworkshop.LocationApi

class StubLocationApi : LocationApi {
    override suspend fun getCurrentLocation() = CurrentLocation(1.05, 1.05)
}
