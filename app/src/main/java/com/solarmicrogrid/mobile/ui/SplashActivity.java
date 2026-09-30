package com.solarmicrogrid.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.auth.SessionManager;
import com.solarmicrogrid.mobile.network.ApiClient;

/**
 * SplashActivity - Dedicated opening splash screen for SolarRays.
 * Features a popup solar logo animation, glowing yellow title glide-in,
 * and smooth transition to Onboarding or Dashboard.
 */
public class SplashActivity extends AppCompatActivity {

    private FrameLayout frameSplashLogo;
    private TextView tvSplashTitle;
    private TextView tvSplashSubtitle;
    private View layoutSplashFooter;
    private View viewGlowBg;
    private View pulseDot;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dark status bar
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.bg_black));

        // Initialize networking
        ApiClient.init(this);

        setContentView(R.layout.activity_splash);

        frameSplashLogo = findViewById(R.id.frameSplashLogo);
        tvSplashTitle = findViewById(R.id.tvSplashTitle);
        tvSplashSubtitle = findViewById(R.id.tvSplashSubtitle);
        layoutSplashFooter = findViewById(R.id.layoutSplashFooter);
        viewGlowBg = findViewById(R.id.viewGlowBg);
        pulseDot = findViewById(R.id.pulseDot);

        // Set initial invisible/scaled down states
        frameSplashLogo.setScaleX(0.08f);
        frameSplashLogo.setScaleY(0.08f);
        frameSplashLogo.setAlpha(0f);

        tvSplashTitle.setAlpha(0f);
        tvSplashTitle.setTranslationY(80f);

        tvSplashSubtitle.setAlpha(0f);
        tvSplashSubtitle.setTranslationY(40f);

        layoutSplashFooter.setAlpha(0f);
        viewGlowBg.setAlpha(0.1f);
        viewGlowBg.setScaleX(0.5f);
        viewGlowBg.setScaleY(0.5f);

        // Run choreographed entrance animations
        startEntranceAnimations();

        // Tap to skip
        findViewById(R.id.splashRoot).setOnClickListener(v -> navigateNext());

        // Auto-navigate after 2.3 seconds
        handler.postDelayed(this::navigateNext, 2300);
    }

    private void startEntranceAnimations() {
        // Step 1: Pop-up Logo with spring overshoot
        handler.postDelayed(() -> {
            frameSplashLogo.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(800)
                    .setInterpolator(new OvershootInterpolator(1.9f))
                    .start();

            viewGlowBg.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .alpha(0.6f)
                    .setDuration(1200)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }, 150);

        // Step 2: Slide up yellow "SolarRays" title
        handler.postDelayed(() -> {
            tvSplashTitle.animate()
                    .translationY(0f)
                    .alpha(1.0f)
                    .setDuration(700)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }, 450);

        // Step 3: Fade in Subtitle
        handler.postDelayed(() -> {
            tvSplashSubtitle.animate()
                    .translationY(0f)
                    .alpha(1.0f)
                    .setDuration(600)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }, 750);

        // Step 4: Fade in footer with pulsing dot
        handler.postDelayed(() -> {
            layoutSplashFooter.animate()
                    .alpha(1.0f)
                    .setDuration(500)
                    .start();

            pulseDot.animate()
                    .scaleX(1.4f)
                    .scaleY(1.4f)
                    .setDuration(600)
                    .withEndAction(() -> {
                        pulseDot.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(600)
                                .start();
                    })
                    .start();
        }, 1000);
    }

    private synchronized void navigateNext() {
        if (isNavigated) return;
        isNavigated = true;

        Intent destination;
        if (SessionManager.getInstance(this).isLoggedIn()) {
            destination = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            destination = new Intent(SplashActivity.this, OnboardingActivity.class);
        }

        startActivity(destination);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
