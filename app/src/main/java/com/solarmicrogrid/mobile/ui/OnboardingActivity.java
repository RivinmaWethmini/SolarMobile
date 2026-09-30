package com.solarmicrogrid.mobile.ui;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.auth.SessionManager;
import com.solarmicrogrid.mobile.models.AuthUser;
import com.solarmicrogrid.mobile.network.ApiClient;

import org.json.JSONObject;

/**
 * OnboardingActivity - Friendly introduction to SolarRays.
 * Clean, user-friendly headlines and smooth transitions.
 */
public class OnboardingActivity extends AppCompatActivity {

    private TextView tvSlideHeadline, tvSlideDescription;
    private TextView btnNextSlide, btnPrevSlide, btnTopSignIn, btnFastDemoProsumer;
    private View dot0, dot1, dot2;
    private Button btnGetStarted, btnSignIn;
    private LinearLayout layoutSlideContent;

    private int currentSlide = 0;
    private final int TOTAL_SLIDES = 3;

    private final String[] headlines = {
            "Share clean solar power with your community",
            "Quick and secure QR pass check-in",
            "Live solar station updates & battery info"
    };
    private final String[] descriptions = {
            "Connect your solar panels or buy green energy easily from your mobile phone.",
            "Get an instant digital pass to check in at any solar station without paperwork.",
            "Check station availability, battery slots, and rates in real-time."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ApiClient.init(this);

        if (SessionManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        layoutSlideContent = findViewById(R.id.layoutSlideContent);
        tvSlideHeadline = findViewById(R.id.tvSlideHeadline);
        tvSlideDescription = findViewById(R.id.tvSlideDescription);

        btnNextSlide = findViewById(R.id.btnNextSlide);
        btnPrevSlide = findViewById(R.id.btnPrevSlide);
        btnTopSignIn = findViewById(R.id.btnTopSignIn);

        dot0 = findViewById(R.id.dot0);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);

        btnGetStarted = findViewById(R.id.btnGetStarted);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnFastDemoProsumer = findViewById(R.id.btnFastDemoProsumer);

        applySlideData(0);
        updateDotIndicators(0, false);

        btnNextSlide.setOnClickListener(v -> {
            if (currentSlide < TOTAL_SLIDES - 1) {
                transitionToSlide(currentSlide + 1, true);
            } else {
                transitionToSlide(0, true);
            }
        });

        btnPrevSlide.setOnClickListener(v -> {
            if (currentSlide > 0) {
                transitionToSlide(currentSlide - 1, false);
            }
        });

        dot0.setOnClickListener(v -> {
            if (currentSlide != 0) transitionToSlide(0, 0 > currentSlide);
        });

        dot1.setOnClickListener(v -> {
            if (currentSlide != 1) transitionToSlide(1, 1 > currentSlide);
        });

        dot2.setOnClickListener(v -> {
            if (currentSlide != 2) transitionToSlide(2, 2 > currentSlide);
        });

        btnGetStarted.setOnClickListener(v -> {
            startActivity(new Intent(OnboardingActivity.this, RegisterActivity.class));
        });

        View.OnClickListener goToLogin = v -> {
            startActivity(new Intent(OnboardingActivity.this, LoginActivity.class));
        };

        btnSignIn.setOnClickListener(goToLogin);
        btnTopSignIn.setOnClickListener(goToLogin);

        if (btnFastDemoProsumer != null) {
            btnFastDemoProsumer.setOnClickListener(v -> performFastProsumerLogin());
        }
    }

    private void transitionToSlide(int targetSlide, boolean isForward) {
        if (targetSlide == currentSlide) return;

        float exitDistance = isForward ? -80f : 80f;
        float enterDistance = isForward ? 80f : -80f;

        layoutSlideContent.animate()
                .translationX(exitDistance)
                .alpha(0f)
                .setDuration(220)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    currentSlide = targetSlide;
                    applySlideData(currentSlide);
                    updateDotIndicators(currentSlide, true);

                    layoutSlideContent.setTranslationX(enterDistance);
                    layoutSlideContent.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(260)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                })
                .start();
    }

    private void applySlideData(int index) {
        tvSlideHeadline.setText(headlines[index]);
        tvSlideDescription.setText(descriptions[index]);

        if (index == 0) {
            btnPrevSlide.setVisibility(View.GONE);
            btnNextSlide.setText("Next →");
        } else if (index == TOTAL_SLIDES - 1) {
            btnPrevSlide.setVisibility(View.VISIBLE);
            btnNextSlide.setText("Start →");
        } else {
            btnPrevSlide.setVisibility(View.VISIBLE);
            btnNextSlide.setText("Next →");
        }
    }

    private void updateDotIndicators(int activeIndex, boolean animate) {
        View[] dots = {dot0, dot1, dot2};
        float density = getResources().getDisplayMetrics().density;
        int activeWidth = (int) (28 * density);
        int inactiveWidth = (int) (8 * density);

        for (int i = 0; i < dots.length; i++) {
            final View dot = dots[i];
            boolean isActive = (i == activeIndex);
            int targetWidth = isActive ? activeWidth : inactiveWidth;

            dot.setBackgroundResource(isActive ? R.drawable.bg_tab_selected : R.drawable.bg_dark_input);

            if (animate) {
                ValueAnimator anim = ValueAnimator.ofInt(dot.getWidth(), targetWidth);
                anim.addUpdateListener(valueAnimator -> {
                    ViewGroup.LayoutParams lp = dot.getLayoutParams();
                    lp.width = (Integer) valueAnimator.getAnimatedValue();
                    dot.setLayoutParams(lp);
                });
                anim.setDuration(200);
                anim.start();
            } else {
                ViewGroup.LayoutParams lp = dot.getLayoutParams();
                lp.width = targetWidth;
                dot.setLayoutParams(lp);
            }
        }
    }

    private void performFastProsumerLogin() {
        Toast.makeText(this, "Connecting as Prosumer...", Toast.LENGTH_SHORT).show();
        ApiClient.login("prosumer@solar.com", "Prosumer@12345", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    String accessToken = json.optString("accessToken", "");
                    String refreshToken = json.optString("refreshToken", "");
                    JSONObject userObj = json.optJSONObject("user");

                    AuthUser authUser = null;
                    if (userObj != null) {
                        authUser = AuthUser.fromJson(userObj);
                    }

                    SessionManager.getInstance(OnboardingActivity.this).saveSession(accessToken, refreshToken, authUser);
                    Toast.makeText(OnboardingActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Toast.makeText(OnboardingActivity.this, "Sign in error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(OnboardingActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
