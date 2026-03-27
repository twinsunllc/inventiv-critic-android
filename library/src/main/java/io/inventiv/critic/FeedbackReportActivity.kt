package io.inventiv.critic

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import io.inventiv.critic.databinding.ActivityFeedbackReportBinding
import kotlinx.coroutines.launch

class FeedbackReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedbackReportBinding
    private var isSubmitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.critic_feedback_title)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.reportSubmitButton.setOnClickListener { createReport() }
    }

    private fun createReport() {
        if (isSubmitting) return

        binding.reportDescriptionLayout.error = null

        val description = binding.reportDescriptionField.text?.toString().orEmpty()
        if (description.isBlank()) {
            binding.reportDescriptionLayout.error = getString(R.string.critic_error_empty_description)
            binding.reportDescriptionField.requestFocus()
            return
        }

        showProgress(true)
        isSubmitting = true

        lifecycleScope.launch {
            try {
                Critic.submitReport(description = description)
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Report creation failed.", e)
                showProgress(false)
                isSubmitting = false
                Snackbar.make(
                    binding.root,
                    getString(R.string.critic_error_submission_failed),
                    Snackbar.LENGTH_LONG,
                ).show()
            }
        }
    }

    private fun showProgress(show: Boolean) {
        val shortAnim = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        binding.reportProgress.visibility = if (show) View.VISIBLE else View.GONE
        binding.reportProgress.animate().setDuration(shortAnim).alpha(if (show) 1f else 0f)

        binding.reportForm.visibility = if (show) View.GONE else View.VISIBLE
        binding.reportForm.animate().setDuration(shortAnim).alpha(if (show) 0f else 1f)
    }

    companion object {
        private const val TAG = "FeedbackReportActivity"
    }
}
