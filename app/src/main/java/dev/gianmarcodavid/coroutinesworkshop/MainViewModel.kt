package dev.gianmarcodavid.coroutinesworkshop

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<UiState>(UiState.Empty)
    val uiState: LiveData<UiState> = _uiState

    private var job: Job? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _uiState.postValue(UiState.Error(makeErrorMessage(throwable)))
    }

    fun onButtonClick() {
        _uiState.value = UiState.Loading

        Log.i("MainViewModel", "Launching coroutine")
        job = viewModelScope.launch(exceptionHandler) {
            try {
                val weather = repository.getCurrentWeather()
                Log.i("MainViewModel", "Got weather")
                _uiState.postValue(UiState.Content(weather))
            } catch (e: Exception) {
                _uiState.postValue(UiState.Error(makeErrorMessage(e)))
            }
            Log.i("MainViewModel", "The coroutine is still alive")
        }
    }

    fun onCancelClick() {
        Log.i("MainViewModel", "Cancelling job $job")
        job?.cancel()
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
