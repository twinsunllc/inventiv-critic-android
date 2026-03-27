package io.inventiv.critic;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import io.inventiv.critic.client.BugReportCreator;
import io.inventiv.critic.model.BugReport;

public class FeedbackReportActivity extends AppCompatActivity {

    private String productAccessToken;

    private ReportTask mReportTask = null;

    private EditText mDescriptionView;
    private View mProgressView;
    private View mReportFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback_report);

        mDescriptionView = findViewById(R.id.report_description_field);
        mProgressView = findViewById(R.id.report_progress);
        mReportFormView = findViewById(R.id.report_form);

        Intent intent = getIntent();
        if( intent == null ) {
            throw new AssertionError("You must pass an Intent with a 'productAccessToken' extra!");
        }

        String productAccessToken = intent.getStringExtra(Critic.INTENT_EXTRA_PRODUCT_ACCESS_TOKEN);
        if(productAccessToken == null) {
            throw new AssertionError("You must pass a 'productAccessToken' extra with your Intent!");
        }
        setProductAccessToken(productAccessToken);

        Button submitButton = (Button) findViewById(R.id.report_submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                createReport();
            }
        });
    }

    private void createReport() {
        if (mReportTask != null) {
            return;
        }

        // Reset errors.
        mDescriptionView.setError(null);

        // Store values at the time of the login attempt.
        String description = mDescriptionView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid description.
        if (TextUtils.isEmpty(description)) {
            mDescriptionView.setError("Please enter a description.");
            focusView = mDescriptionView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mReportTask = new ReportTask(description);
            mReportTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mReportFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mReportFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mReportFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mReportFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class ReportTask extends AsyncTask<Void, Void, Boolean> {

        private final String mDescription;

        ReportTask(String description) {
            mDescription = description;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                BugReport bugReport = new BugReportCreator()
                        .description(mDescription)
                .create(FeedbackReportActivity.this);
                System.out.println( "new report created: " + bugReport.getDescription() );
            } catch (Throwable t) {
                t.printStackTrace();
                Log.e(FeedbackReportActivity.class.getSimpleName(), "Report creation failed.", t);
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mReportTask = null;
            showProgress(false);

            if (success) {
                finish();
            } else {
                Log.w(FeedbackReportActivity.class.getSimpleName(), "Something went wrong with Report creation.");
            }
        }

        @Override
        protected void onCancelled() {
            mReportTask = null;
            showProgress(false);
        }
    }

    private String getProductAccessToken() {
        return productAccessToken;
    }

    private void setProductAccessToken(String productAccessToken) {
        this.productAccessToken = productAccessToken;
    }
}
