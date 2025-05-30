package com.example.eduease;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog; // Import AlertDialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Settings extends AppCompatActivity {

    private ImageView profileImage;
    private EditText changePassword;
    private TextView account;
    private Button saveButton;
    private Button logoutButton;

    // Declare a Vibrator instance to avoid repeated getSystemService calls
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        // Initialize views
        profileImage = findViewById(R.id.profile_image);
        changePassword = findViewById(R.id.change_password);
        account = findViewById(R.id.account);
        saveButton = findViewById(R.id.save_btn);
        logoutButton = findViewById(R.id.logout_btn);

        // Initialize Vibrator service once
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Load profile image
        loadProfileImage();

        // Display user information
        displayUserInfo();

        // Set up vibration feedback for relevant views
        // Note: The setupVibration method creates a new OnClickListener,
        // which might override other listeners if applied to the same view.
        // For buttons like saveButton and logoutButton, their specific listeners
        // already have vibration.
        setupVibration(profileImage); // Only apply to profileImage if it's the only one without another click listener
        // If changePassword is meant to be editable, adding a click listener for vibration might interfere.
        // Consider if changePassword or account really need a simple click vibration.

        // Set up the save button listener
        setupSaveButtonListener();

        // Set up the logout button listener
        setupLogoutButtonListener();

        // Adjust view for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadProfileImage() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .apply(new RequestOptions()
                            .circleCrop()
                            .override(192, 192)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.person)
                            .error(R.drawable.person))
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.person);
        }
    }

    private void displayUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            account.setText(user.getEmail());
            changePassword.setText("*******"); // Display placeholder for password
            // Optionally, you might want to make changePassword editable or show a button to enable editing
            changePassword.setEnabled(true); // Ensure it's enabled if user needs to change it
        } else {
            Toast.makeText(this, "No user information found", Toast.LENGTH_SHORT).show();
            account.setText("N/A"); // Default text for no user
            changePassword.setText(""); // Clear password field
            changePassword.setEnabled(false); // Disable if no user
        }
    }

    // Consolidated vibration method for reuse
    private void triggerVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }

    // This method now uses the shared triggerVibration
    private void setupVibration(View... views) {
        for (View view : views) {
            view.setOnClickListener(v -> triggerVibration());
        }
    }

    private void setupSaveButtonListener() {
        saveButton.setOnClickListener(v -> {
            triggerVibration(); // Trigger vibration when save button is clicked

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String newPassword = changePassword.getText().toString().trim();

                if (!newPassword.isEmpty() && !newPassword.equals("*******")) { // Check if it's not empty AND not the placeholder
                    user.updatePassword(newPassword).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                            changePassword.setText("*******"); // Reset to placeholder after update
                            changePassword.clearFocus(); // Remove focus from the EditText
                            // Optionally, if you disable editing until another button press:
                            // changePassword.setEnabled(false);
                        } else {
                            // Firebase authentication requires recent login for password changes
                            // Handle cases where the user needs to re-authenticate
                            String errorMessage = "Failed to update password: " + task.getException().getMessage();
                            if (task.getException() != null && task.getException().getMessage() != null &&
                                    task.getException().getMessage().contains("recent login")) {
                                errorMessage = "Password update requires recent login. Please log out and log in again, then try changing your password.";
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                } else if (newPassword.equals("*******")) {
                    Toast.makeText(this, "No new password entered.", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Password cannot be empty.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No authenticated user found. Please log in.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLogoutButtonListener() {
        logoutButton.setOnClickListener(v -> {
            triggerVibration(); // Trigger vibration when logout button is clicked

            // Show confirmation dialog before logging out
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Logout")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        // User confirmed logout
                        FirebaseAuth.getInstance().signOut();
                        Toast.makeText(Settings.this, "Logged out successfully!", Toast.LENGTH_SHORT).show();

                        // Navigate to MainActivity with a flag to clear back stack
                        Intent intent = new Intent(Settings.this, MainActivity.class);
                        intent.putExtra("from_settings", true); // Optional flag for MainActivity if needed
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear all previous activities
                        startActivity(intent);

                        // Finish current activity
                        finish();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        // User cancelled logout, do nothing
                        dialog.dismiss();
                    })
                    .show();
        });
    }
}