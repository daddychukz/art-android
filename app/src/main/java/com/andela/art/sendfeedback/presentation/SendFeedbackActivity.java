package com.andela.art.sendfeedback.presentation;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import android.view.View;

import com.andela.art.R;
import com.andela.art.databinding.ActivitySendFeedbackBinding;
import com.andela.art.models.SendFeedback;
import com.andela.art.root.ApplicationComponent;
import com.andela.art.root.ApplicationModule;
import com.andela.art.root.ArtApplication;
import com.andela.art.sendfeedback.injection.DaggerSendFeedbackComponent;
import com.andela.art.sendfeedback.injection.SendFeedbackModule;
import com.google.firebase.auth.FirebaseAuth;

import javax.inject.Inject;

/**
 * Send feedback activity.
 */
public class SendFeedbackActivity extends AppCompatActivity implements SendFeedbackView {
    @Inject
    SendFeedbackPresenter sendFeedbackPresenter;

    ActivitySendFeedbackBinding feedbackBinding;
    ApplicationComponent applicationComponent;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        feedbackBinding = DataBindingUtil
                .setContentView(this, R.layout.activity_send_feedback);
        applicationComponent = ((ArtApplication) getApplication())
                .applicationComponent();
        initializeSendFeedbackComponent();
        sendFeedbackPresenter.attachView(this);
        mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Initialize Report Problem Component.
     */
    private void initializeSendFeedbackComponent() {
        DaggerSendFeedbackComponent.builder()
                .applicationComponent(applicationComponent)
                .applicationModule(new ApplicationModule(getApplication()))
                .sendFeedbackModule(new SendFeedbackModule())
                .build()
                .inject(this);
    }

    /**
     * Send Feedback.
     *
     * @param view The view that calls this method is an ImageView
     */
    public void sendFeedback(View view) {
        String email = mAuth.getCurrentUser().getEmail();
        String message = feedbackBinding.seedFeedbackText.getText().toString().trim();
        String reportType = "feedback";

        if (message.isEmpty()) {
            Toast.makeText(this, "Please provide feedback", Toast.LENGTH_LONG).show();
            return;
        }

        sendFeedbackPresenter.sendFeedback(email, message, reportType);
        finish();
    }

    /**
     * Success on call of an endpoint.
     * @param sendFeedback reportProblem model
     */
    public void sendFeedbackSuccess(SendFeedback sendFeedback) {
        Toast.makeText(
                this,
                "Feedback sent successfully",
                Toast.LENGTH_LONG).show();

        sendFeedbackPresenter.dispose();
    }

    /**
     * Show error resulted from call of the endpoint.
     *
     * @param e throwable exception
     */
    public void sendFeedbackError(Throwable e) {
        Toast.makeText(
                this,
                "Report not submitted. Please try again",
                Toast.LENGTH_LONG).show();
    }

    /**
     * Cancel sending of feedback.
     *
     * @param view The view that calls this method is an ImageView
     */
    public void closeFeedback(View view) {
        super.onBackPressed();
        finish();
    }
}
