package dev.gianmarcodavid.coroutinesworkshop

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import dagger.hilt.android.AndroidEntryPoint
import dev.gianmarcodavid.coroutinesworkshop.databinding.ActivityMainBinding
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            onButtonClick()
        }
    }

    private fun displayText(text: String) {
        binding.textView.text = text
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.isInvisible = !show
    }

    private fun onButtonClick() {
        showLoading(true)
        displayText("Starting a couple of long tasks")
        val aResult = aLongTask()
        val anotherResult = anotherLongTask()
        val combinedResult = "$aResult $anotherResult"
        showLoading(false)
        displayText("The tasks completed with: $combinedResult")
    }

    private suspend fun aLongTask(): String {
        delay(2000)
        return "Hi"
    }

    private suspend fun anotherLongTask(): String {
        delay(1000)
        return "everyone"
    }
}
