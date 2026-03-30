package io.inventiv.critic.example

import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.inventiv.critic.Critic
import io.inventiv.critic.example.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnInitialize.setOnClickListener { onInitialize() }
        binding.btnSubmitReport.setOnClickListener { onSubmitReport() }
        binding.btnShowFeedback.setOnClickListener { onShowFeedback() }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
        binding.editApiToken.clearFocus()
        binding.editBaseUrl.clearFocus()
    }

    private fun onInitialize() {
        hideKeyboard()
        val apiToken = binding.editApiToken.text?.toString()?.trim().orEmpty()
        val host = binding.editBaseUrl.text?.toString()?.trim().orEmpty()

        if (apiToken.isBlank()) {
            appendStatus("Error: API token is required.")
            return
        }

        if (Critic.isInitialized) {
            appendStatus("Critic is already initialized.")
            return
        }

        appendStatus("Initializing Critic SDK...")
        binding.btnInitialize.isEnabled = false

        try {
            Critic.initialize(
                context = application,
                apiToken = apiToken,
                host = host.ifBlank { null },
            )
            appendStatus("Critic.initialize() called successfully. Ping is running in the background.")
            binding.btnSubmitReport.isEnabled = true
            binding.btnShowFeedback.isEnabled = true
        } catch (e: Exception) {
            appendStatus("Initialize failed: ${e.message}")
            binding.btnInitialize.isEnabled = true
        }
    }

    private fun onSubmitReport() {
        hideKeyboard()
        if (!Critic.isInitialized) {
            appendStatus("Error: Critic is not initialized.")
            return
        }

        appendStatus("Submitting test bug report...")
        binding.btnSubmitReport.isEnabled = false

        lifecycleScope.launch {
            try {
                val report = Critic.submitReport(
                    description = "Test bug report from the example app",
                    stepsToReproduce = "1. Opened example app\n2. Tapped Submit Test Report",
                    userIdentifier = "example-app-user",
                )
                appendStatus("Report submitted successfully! ID: ${report.id}")
            } catch (e: Exception) {
                appendStatus("Submit failed: ${e.message}")
            } finally {
                binding.btnSubmitReport.isEnabled = true
            }
        }
    }

    private fun onShowFeedback() {
        if (!Critic.isInitialized) {
            appendStatus("Error: Critic is not initialized.")
            return
        }
        Critic.showFeedbackReportActivity()
    }

    private fun appendStatus(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val current = binding.textStatus.text?.toString().orEmpty()
        val updated = if (current.isBlank()) {
            "[$timestamp] $message"
        } else {
            "$current\n[$timestamp] $message"
        }
        binding.textStatus.text = updated
    }
}
