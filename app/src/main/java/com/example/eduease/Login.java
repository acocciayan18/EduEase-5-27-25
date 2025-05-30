package com.example.eduease;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog; // Import AlertDialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Login extends BaseActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 100;

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;

    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private ImageButton googleLoginButton;
    private TextView signUpTextButton;
    private TextView forgotPasswordButton;

    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        applyEdgeToEdgePadding();

        firebaseAuth = FirebaseAuth.getInstance();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        initializeUI();
        setListeners();
        setupToggleForPasswordField(passwordInput);
    }

    private void applyEdgeToEdgePadding() {
        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initializeUI() {
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        googleLoginButton = findViewById(R.id.google_login_button);
        signUpTextButton = findViewById(R.id.signup_text_button);
        forgotPasswordButton = findViewById(R.id.forgot_pass);
    }

    private void setListeners() {
        loginButton.setOnClickListener(v -> {
            vibratePhone();
            signInWithEmail();
        });

        googleLoginButton.setOnClickListener(v -> {
            vibratePhone();
            signInWithGoogle();
        });

        signUpTextButton.setOnClickListener(v -> {
            vibratePhone();
            openSignUpActivity();
        });

        forgotPasswordButton.setOnClickListener(v -> {
            vibratePhone();
            sendPasswordResetEmail();
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupToggleForPasswordField(EditText passwordField) {
        passwordField.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableEndIndex = 2;
                if (passwordField.getCompoundDrawables()[drawableEndIndex] != null &&
                        event.getRawX() >= (passwordField.getRight() - passwordField.getCompoundDrawables()[drawableEndIndex].getBounds().width())) {
                    togglePasswordVisibility(passwordField);
                    passwordField.performClick();
                    return true;
                }
            }
            return false;
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void togglePasswordVisibility(EditText passwordField) {
        boolean isPasswordVisible = (passwordField.getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD;
        passwordField.setInputType(isPasswordVisible ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordField.setCompoundDrawablesWithIntrinsicBounds(
                passwordField.getCompoundDrawables()[0],
                passwordField.getCompoundDrawables()[1],
                getDrawable(isPasswordVisible ? R.drawable.show_password : R.drawable.hide_password),
                passwordField.getCompoundDrawables()[3]
        );
        passwordField.setSelection(passwordField.getText().length());
    }

    private void vibratePhone() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }

    private void sendPasswordResetEmail() {
        String email = emailInput.getText().toString().trim();

        if (email.isEmpty()) {
            showToast("Please enter your email address in the email field.");
            emailInput.setError("Email required");
            emailInput.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            showToast("Please enter a valid email address.");
            emailInput.setError("Invalid email format");
            emailInput.requestFocus();
            return;
        }

        showLoading();

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    hideLoading();

                    if (task.isSuccessful()) {
                        showToast("Password reset email sent to " + email + ". Please check your inbox.");
                    } else {
                        String errorMessage = "Failed to send reset email. ";
                        if (task.getException() != null) {
                            errorMessage += task.getException().getMessage();
                            Log.e(TAG, "Password reset failed: " + task.getException().getMessage(), task.getException());
                        } else {
                            errorMessage += "An unknown error occurred.";
                        }
                        showToast(errorMessage);
                    }
                });
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }


    private void signInWithEmail() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showToast("Please enter both email and password.");
            return;
        }

        if (!isValidEmail(email)) {
            showToast("Please enter a valid email address.");
            emailInput.setError("Invalid email format.");
            emailInput.requestFocus();
            return;
        }

        showLoading();

        // --- MODIFIED LOGIC HERE ---
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    hideLoading();

                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user == null) {
                            showToast("Login failed. User object is null.");
                            return;
                        }

                        if (user.isEmailVerified()) {
                            updateUI(user);
                        } else {
                            // Account exists but email is not verified
                            showVerificationDialog(user); // Show the new dialog
                            firebaseAuth.signOut(); // Sign out the unverified user
                        }
                    } else {
                        // Login failed (e.g., wrong password, user not found, account disabled)
                        String message = "Login failed. Please check your email and password."; // Generic error message
                        if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                            // This covers "user-not-found" and "wrong-password" errors.
                            message = "Invalid email or password.";
                        } else if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                            // This covers "user-disabled"
                            message = "Your account has been disabled.";
                        } else if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                            // This indicates the email exists but with a different provider (e.g., Google)
                            message = "This email is registered with Google. Please sign in using Google.";
                        }
                        showToast(message);
                        Log.e(TAG, "Email login failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
        // --- END MODIFIED LOGIC ---
    }

    /**
     * Shows a dialog instructing the user to verify their email.
     * Offers options to resend the verification link or proceed after manual verification.
     */
    private void showVerificationDialog(FirebaseUser user) {
        new AlertDialog.Builder(this)
                .setTitle("Email Verification Required")
                .setMessage("Your email address is not verified. Please check your inbox for a verification link." +
                        "\n\nAfter verifying, you can try logging in again.")
                .setPositiveButton("Resend Link", (dialog, which) -> {
                    // Vibrate for feedback
                    vibratePhone();
                    // Resend verification email
                    user.sendEmailVerification()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    showToast("Verification email sent! Please check your inbox and spam folder.");
                                } else {
                                    showToast("Failed to resend verification email: " + task.getException().getMessage());
                                    Log.e(TAG, "Failed to resend verification email", task.getException());
                                }
                            });
                })
                .setNegativeButton("OK", (dialog, which) -> {
                    // User clicked OK, just dismiss the dialog.
                    // They will have to log in again after verifying.
                    vibratePhone();
                    dialog.dismiss();
                })
                // Allow cancelling the dialog by tapping outside or pressing back
                .setCancelable(true)
                .show();
    }


    private void signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            ActivityOptions options = ActivityOptions.makeBasic();
            startActivityForResult(signInIntent, RC_SIGN_IN, options.toBundle());
        });
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        showLoading();
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    hideLoading();

                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user == null || user.getEmail() == null) {
                            showToast("Google account error. Try again.");
                            return;
                        }

                        // For Google login, Firebase automatically verifies the email
                        // if it comes from a trusted Google source.
                        // So, we don't need to check user.isEmailVerified() here.

                        String email = user.getEmail();

                        boolean hasPassword = false;
                        for (com.google.firebase.auth.UserInfo info : user.getProviderData()) {
                            if (info.getProviderId().equals("password")) {
                                hasPassword = true;
                                break;
                            }
                        }

                        if (!hasPassword) {
                            Intent intent = new Intent(Login.this, GooglePasswordSetup.class);
                            intent.putExtra("EMAIL", email);
                            intent.putExtra("NAME", user.getDisplayName());
                            startActivity(intent);
                            finish();
                        } else {
                            updateUI(user);
                        }

                    } else {
                        Log.w(TAG, "firebaseAuthWithGoogle:failure", task.getException());
                        if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                            showToast("This email is already registered with an email/password. Please log in with email/password or use a different Google account.");
                        } else {
                            showToast("Google Login failed. " + (task.getException() != null ? task.getException().getMessage() : "Try again."));
                        }
                    }
                });
    }

    private void openSignUpActivity() {
        Intent createAccountIntent = new Intent(Login.this, CreateAccount.class);
        createAccountIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(createAccountIntent);
        finish();
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent homeIntent = new Intent(Login.this, Home.class);
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account != null && account.getPhotoUrl() != null) {
                homeIntent.putExtra("PROFILE_IMAGE_URL", account.getPhotoUrl().toString());
            }

            homeIntent.putExtra("SKIP_MUSIC", true);
            startActivity(homeIntent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                } else {
                    showToast("Google sign-in cancelled or failed.");
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google Login failed: ", e);
                showToast("Google Login failed. Please try again. Error: " + e.getStatusCode());
            }
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(Login.this, message, Toast.LENGTH_SHORT).show());
    }
}