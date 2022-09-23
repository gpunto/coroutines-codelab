package dev.gianmarcodavid.coroutinesworkshop

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.lifecycle.LifecycleEventObserver
import dagger.hilt.android.AndroidEntryPoint
import dev.gianmarcodavid.coroutinesworkshop.MainViewModel.UiState
import dev.gianmarcodavid.coroutinesworkshop.databinding.ActivityMainBinding

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            viewModel.onButtonClick()
        }
        binding.cancelButton.setOnClickListener {
            finish()
        }
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            Log.i("MainActivity", "Current lifecycle state: ${event.name}")
        })

        viewModel.uiState.observe(this) { state ->
            showLoading(false)

            when (state) {
                is UiState.Content -> showWeather(state.weather)
                UiState.Empty -> binding.textView.setText(R.string.empty_message)
                UiState.Loading -> showLoading(true)
                is UiState.Error -> binding.textView.text = state.message
            }
        }
    }

    private fun showWeather(weather: Weather) {
        binding.textView.text = getString(
            R.string.weather_template,
            weather.temperature.toString(),
            weather.windSpeed.toString()
        )
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.isInvisible = !show
    }
}
