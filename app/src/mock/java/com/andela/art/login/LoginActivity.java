package com.andela.art.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.andela.art.R;
import com.andela.art.root.ApplicationComponent;
import com.andela.art.databinding.ActivityLoginBinding;
import com.andela.art.login.injection.DaggerLoginComponent;
import com.andela.art.login.injection.LoginModule;
import com.andela.art.root.ApplicationModule;
import com.andela.art.securitydashboard.presentation.SecurityDashboardActivity;
import com.andela.art.root.ArtApplication;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * LoginActivity handles the login of user into the application.
 */

public class LoginActivity extends AppCompatActivity implements SecurityEmailsView {

    @Inject GoogleSignInClient mGoogleSignInClient;
    FirebaseAuth mAuth;
    private static final int RC_SIGN_IN = 2;
    static final String TAG = "LoginActivity";
    FirebaseAuth.AuthStateListener mAuthListener;
    // A progress dialog to display when the user is connecting in
    // case there is a delay in any of the dialogs being ready.
    ProgressDialog mConnectionProgressDialog;
    Intent dashboard;

    // Custom error code for unauthorized GMail addresses
    private static final int UNAUTHORIZED_CODE = 14672;

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ApplicationComponent applicationComponent = ((ArtApplication) getApplication())
                .applicationComponent();
        DaggerLoginComponent.builder()
                .applicationComponent(applicationComponent)
                .applicationModule(new ApplicationModule(getApplication()))
                .loginModule(new LoginModule())
                .build()
                .inject(this);
        dashboard = new Intent(LoginActivity.this, SecurityDashboardActivity.class);
        ActivityLoginBinding activityLoginBinding = DataBindingUtil
                .setContentView(this, R.layout.activity_login);
        activityLoginBinding.googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

        // Instance of mAuth
        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() != null) {
                    // Hide the progress dialog if its showing.
                    mConnectionProgressDialog.dismiss();
                    // Add check if user is admin here in future
                    LoginActivity.this.startActivity(dashboard);
                }
            }
        };

        // Configure the ProgressDialog that will be shown if there is a
        // delay in presenting the user with the next sign in step.
        mConnectionProgressDialog = new ProgressDialog(this);
        mConnectionProgressDialog.setMessage("Signing in...");
    }

    /**
     * Log in with google.
     * @param account - google account
     */
    public void firebasewithGoogleAuth(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider
                .getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            // Add check if user is admin here in future
                            startActivity(dashboard);
                        } else {
                            // Hide the progress dialog if its showing.
                            mConnectionProgressDialog.dismiss();
                            // If sign in fails, display a message to the user.
                            Toast.makeText(LoginActivity.this,
                                    "Authentication Failed: "
                                            + "Ensure you have an internet connection.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });

    }

    /**
     * Sign in and redirect.
     */
    public void signIn() {
        // Show the dialog as we are now signing in.
        mConnectionProgressDialog.show();
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                String emailDomain = account.getEmail().split("@")[1];

                // filter out Andela email addresses
                if ("andela.com".equals(emailDomain)) {

                    Toast.makeText(this, "Andela email", Toast.LENGTH_SHORT).show();

                } else {

                    // filter out only specific GMail addresses assigned to the guards
                    if (isAllowedNonAndelaEmail(account.getEmail())) {
                        Toast.makeText(this, "Allowed non Andela email",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        mGoogleSignInClient.signOut();
                        throw new ApiException(new Status(UNAUTHORIZED_CODE));
                    }
                }

                firebasewithGoogleAuth(account);

            } catch (ApiException e) {
                // Hide the progress dialog if its showing.
                mConnectionProgressDialog.dismiss();
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                if (e.getStatusCode() == UNAUTHORIZED_CODE) {
                    Toast.makeText(LoginActivity.this,
                            "Please use your Andela email address",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Authorization Failed: Ensure you have selected a Google account.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Check to see if a given non Andela GMail address is among the ones allowed to login.
     *
     * @param email Email address for the user
     * @return boolean true if an email is allowed, false if the email is not allowed
     */
    public boolean isAllowedNonAndelaEmail(String email) {
        List<String> allowedEmailAddresses = new ArrayList<>();
        allowedEmailAddresses.add("muhallan1@gmail.com");
        allowedEmailAddresses.add("chadwalt04@gmail.com");

        return allowedEmailAddresses.contains(email);


    }


    /**
     * @param emails - list of emails.
     */
    @Override
    public void populateEmailList(List<String> emails) {

    }
}
