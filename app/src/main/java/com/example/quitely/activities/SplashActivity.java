package com.example.quitely.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quitely.MainActivity;
import com.example.quitely.R;
import com.example.quitely.notifications.NotificationHelper;
import com.example.quitely.utils.PreferenceManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Notification Channels
        NotificationHelper.createNotificationChannels(this);

        ImageView ivLogo = findViewById(R.id.ivSplashLogo);
        TextView tvName = findViewById(R.id.tvSplashAppName);
        TextView tvDesc = findViewById(R.id.tvSplashDescription);
        TextView tvTagline = findViewById(R.id.tvSplashTagline);

        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(800);
        ivLogo.startAnimation(fadeIn);
        tvName.startAnimation(fadeIn);
        tvDesc.startAnimation(fadeIn);
        tvTagline.startAnimation(fadeIn);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            PreferenceManager pref = PreferenceManager.getInstance(this);
            Intent nextIntent;
            if (pref.isOnboardingCompleted()) {
                nextIntent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                nextIntent = new Intent(SplashActivity.this, OnboardingActivity.class);
            }
            startActivity(nextIntent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 1200);
    }
}
