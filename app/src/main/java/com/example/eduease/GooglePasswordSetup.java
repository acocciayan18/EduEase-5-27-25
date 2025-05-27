package com.example.eduease;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class GooglePasswordSetup extends BaseActivity {

    private EditText passwordEditText, repasswordEditText;
    private Button submitButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private boolean passwordLinked = false; // to track if password was submitted

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.google_provide_passsword); // assuming this is your XML name

        passwordEditText = findViewById(R.id.password);
        repasswordEditText = findViewById(R.id.repassword);
        submitButton = findViewById(R.id.sign_up_button);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(this, "No authenticated user found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        submitButton.setOnClickListener(view -> linkPassword());
    }

    private void linkPassword() {
        String password = passwordEditText.getText().toString().trim();
        String repassword = repasswordEditText.getText().toString().trim();

        if (!password.equals(repassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading();

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);

        currentUser.linkWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                hideLoading();

                passwordLinked = true;
//                Toast.makeText(this, "Password linked successfully!", Toast.LENGTH_SHORT).show();

                // Go to main app screen or login screen
                Intent intent = new Intent(GooglePasswordSetup.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {

                hideLoading();
                Toast.makeText(this, "Failed to link password: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        showLoading();

        // Delete account if user exits before submitting password
        if (!passwordLinked && currentUser != null) {
            currentUser.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    hideLoading();
                    Toast.makeText(this, "Account removed. Please complete sign up next time.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        // Confirm if user really wants to exit
        new AlertDialog.Builder(this)
                .setTitle("Cancel Signup?")
                .setMessage("If you go back now, your account will be removed.")
                .setPositiveButton("Exit", (dialog, which) -> {
                    super.onBackPressed(); // triggers onDestroy
                })
                .setNegativeButton("Stay", null)
                .show();
    }
}
