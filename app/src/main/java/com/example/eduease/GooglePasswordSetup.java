package com.example.eduease;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class GooglePasswordSetup extends BaseActivity {

    private EditText passwordEditText, repasswordEditText;
    private Button submitButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private boolean passwordLinked = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.google_provide_passsword);

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
            hideLoading();
            if (task.isSuccessful()) {
                passwordLinked = true;

                // Optional: reload the user if needed
                currentUser.reload().addOnCompleteListener(reloadTask -> {
                    Intent intent = new Intent(GooglePasswordSetup.this, Home.class);
                    startActivity(intent);
                    finish();
                });

            } else {
                Toast.makeText(this, "Failed to link password: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!passwordLinked && currentUser != null) {
            showLoading();
            currentUser.delete().addOnCompleteListener(task -> {
                hideLoading();
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Account removed. Please complete sign up next time.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to delete account: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Signup?")
                .setMessage("If you go back now, your account will be removed.")
                .setPositiveButton("Exit", (dialog, which) -> GooglePasswordSetup.super.onBackPressed())
                .setNegativeButton("Stay", null)
                .show();
    }
}
