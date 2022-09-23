package dev.gianmarcodavid.coroutinesworkshop

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import dagger.hilt.android.AndroidEntryPoint
import dev.gianmarcodavid.coroutinesworkshop.databinding.ActivityMainBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            onButtonClickConcurrentAsync()
        }
    }

    private fun setState(loading: Boolean, text: String) {
        binding.progressBar.isInvisible = !loading
        binding.textView.text = text
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.isInvisible = !show
    }

    // Using runBlocking -> it runs but it blocks the main thread
    private fun onButtonClickRunBlocking() {
        runBlocking {
            setState(loading = true, text = "Starting a couple of long tasks")
            val aResult = aLongTask()
            val anotherResult = anotherLongTask()
            val combinedResult = "$aResult $anotherResult"
            setState(loading = false, text = "The tasks completed with: $combinedResult")
        }
    }

    // Using launch -> it runs and the main thread is not blocked
    private fun onButtonClickLaunch() {
        MainScope().launch {
            setState(loading = true, text = "Starting a couple of long tasks")
            val aResult = aLongTask()
            val anotherResult = anotherLongTask()
            val combinedResult = "$aResult $anotherResult"
            setState(loading = false, text = "The tasks completed with: $combinedResult")
        }
    }

    // Using launch to make the inner coroutines concurrent -> it works but we can't retrieve their results
    private fun onButtonClickConcurrentLaunch() {
        MainScope().launch {
            setState(loading = true, text = "Starting a couple of long tasks")
            val aResult = launch { aLongTask() }
            val anotherResult = launch { anotherLongTask() }
            val combinedResult = "$aResult $anotherResult"
            setState(loading = false, text = "The tasks completed with: $combinedResult")
        }
    }

    // Using async to make the inner coroutines concurrent -> it works and we can retrieve their results
    private fun onButtonClickConcurrentAsync() {
        MainScope().launch {
            setState(loading = true, text = "Starting a couple of long tasks")
            val aResult = async { aLongTask() }
            val anotherResult = async { anotherLongTask() }
            val combinedResult = "${aResult.await()} ${anotherResult.await()}"
            setState(loading = false, text = "The tasks completed with: $combinedResult")
        }
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
