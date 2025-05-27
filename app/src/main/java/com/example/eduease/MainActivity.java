package com.example.eduease;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private boolean isFreshLaunch = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (refreshedUser != null && isPasswordAccount(refreshedUser)) {
                        Intent homeIntent = new Intent(this, Home.class);
                        homeIntent.putExtra("user_email", refreshedUser.getEmail());
                        homeIntent.putExtra("user_name", refreshedUser.getDisplayName());

                        String profileImageUrl = (refreshedUser.getPhotoUrl() != null) ?
                                refreshedUser.getPhotoUrl().toString() : "";
                        homeIntent.putExtra("PROFILE_IMAGE_URL", profileImageUrl);

                        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(homeIntent);
                        finish();
                    } else {
                        continueToMain();
                    }
                } else {
                    FirebaseAuth.getInstance().signOut();
                    continueToMain();
                }
            });
        } else {
            continueToMain();
        }
    }

    private boolean isPasswordAccount(FirebaseUser user) {
        for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
            if ("password".equals(profile.getProviderId())) {
                return true;
            }
        }
        return false;
    }

    private void continueToMain() {
        boolean fromSettings = getIntent().getBooleanExtra("from_settings", false);
        if (isFreshLaunch && !fromSettings) {
            playOpeningSound();
        }
        setContentView(R.layout.activity_main);
        applyEdgeToEdgePadding();
        setupGetStartedButton();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        isFreshLaunch = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void playOpeningSound() {
        mediaPlayer = MediaPlayer.create(this, R.raw.opening_music);
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.release();
            mediaPlayer = null;
        });
        mediaPlayer.start();
    }

    private void applyEdgeToEdgePadding() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupGetStartedButton() {
        Button getStartedBtn = findViewById(R.id.getStarted_btn);
        getStartedBtn.setOnClickListener(view -> {
            vibratePhone();
            navigateToCreateAccount();
        });
    }

    private void vibratePhone() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }

    private void navigateToCreateAccount() {
        Intent intent = new Intent(this, CreateAccount.class);
        startActivity(intent);
    }
}
