package com.solarmicrogrid.mobile.ui;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.auth.SessionManager;
import com.solarmicrogrid.mobile.models.AuthUser;
import com.solarmicrogrid.mobile.network.ApiClient;

import org.json.JSONObject;

/**
 * OnboardingActivity - Friendly introduction to SolarRays.
 * Clean, user-friendly headlines and smooth transitions matching reference design.
 */
public class OnboardingActivity extends AppCompatActivity {

    private TextView tvSlideHeadline, tvSlideDescription;
    private TextView btnNextSlide, btnPrevSlide, btnTopSignIn;
    private View dot0, dot1, dot2;
    private View dash0, dash1, dash2;
    private Button btnGetStarted, btnSignIn;
    private LinearLayout layoutSlideContent;

    private int currentSlide = 0;
    private final int TOTAL_SLIDES = 3;

    private final String[] headlines = {
            "<font color='#FFD000'>Solar panels</font><br/><font color='#FFFFFF'>reduce climate change</font>",
            "<font color='#FFD000'>Smart Battery</font><br/><font color='#FFFFFF'>storage &amp; microgrid</font>",
            "<font color='#FFD000'>Clean Energy</font><br/><font color='#FFFFFF'>trade with community</font>"
    };
    private final String[] descriptions = {
            "Solar panel monitoring systems gather data from various sensors and meters installed within the solar PV system.",
            "Real-time automated phase synchronization, zero fossil fuel backup, and secure QR-verified dispatch.",
            "Connect your solar panels, reserve battery capacity, and earn green credits seamlessly from your phone."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ApiClient.init(this);

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

        dash0 = findViewById(R.id.dash0);
        dash1 = findViewById(R.id.dash1);
        dash2 = findViewById(R.id.dash2);

        btnGetStarted = findViewById(R.id.btnGetStarted);
        btnSignIn = findViewById(R.id.btnSignIn);

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvSlideHeadline.setText(Html.fromHtml(headlines[index], Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvSlideHeadline.setText(Html.fromHtml(headlines[index]));
        }
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
            if (dot == null) continue;
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

        // Also update top stories dash indicators
        View[] dashes = {dash0, dash1, dash2};
        int yellowColor = ContextCompat.getColor(this, R.color.yellow_primary);
        int inactiveDashColor = 0x55FFFFFF;
        for (int i = 0; i < dashes.length; i++) {
            if (dashes[i] != null) {
                dashes[i].setBackgroundTintList(ColorStateList.valueOf(i == activeIndex ? yellowColor : inactiveDashColor));
            }
        }
    }
}
