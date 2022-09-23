package dev.gianmarcodavid.coroutinesworkshop

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.concurrent.thread

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<UiState>(UiState.Empty)
    val uiState: LiveData<UiState> = _uiState

    fun onButtonClick() {
        _uiState.value = UiState.Loading

        thread {
            try {
                val weather = repository.getCurrentWeather()
                _uiState.postValue(UiState.Content(weather))
            } catch (e: Exception) {
                _uiState.postValue(UiState.Error(makeErrorMessage(e)))
            }
        }
    }

    private fun makeErrorMessage(t: Throwable): String =
        "Got an exception: ${t.message ?: t::class.simpleName.orEmpty()}"

    sealed class UiState {
        object Empty : UiState()
        object Loading : UiState()
        data class Content(val weather: Weather) : UiState()
        data class Error(val message: String) : UiState()
    }
}
