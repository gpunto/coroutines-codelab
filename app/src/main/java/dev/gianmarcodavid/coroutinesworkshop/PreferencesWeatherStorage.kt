package dev.gianmarcodavid.coroutinesworkshop

import android.content.SharedPreferences
import android.os.Looper
import androidx.core.content.edit
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class PreferencesWeatherStorage @Inject constructor(private val preferences: SharedPreferences) : WeatherStorage {

    override fun store(weather: Weather) {
        requireBackgroundThread()

        preferences.edit(commit = true) {
            putString(KEY_WEATHER, Json.encodeToString(weather))
        }
    }

    override fun get(): Weather? {
        requireBackgroundThread()

        return preferences.getString(KEY_WEATHER, null)
            ?.let(Json.Default::decodeFromString)
    }

    companion object {
        private const val KEY_WEATHER = "weather"

        private fun requireBackgroundThread() {
            check(Looper.myLooper() != Looper.getMainLooper()) {
                "I/O not allowed on the main thread"
            }
        }
    }
}
