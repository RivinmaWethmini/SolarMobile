package com.solarmicrogrid.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.auth.SessionManager;
import com.solarmicrogrid.mobile.models.AuthUser;
import com.solarmicrogrid.mobile.network.ApiClient;

import org.json.JSONObject;

/**
 * Onboarding Carousel introducing the Solarrays Microgrid trading capabilities.
 * Features an animated brand entrance and 1-tap instant test authentication.
 */
public class OnboardingActivity extends AppCompatActivity {

    private TextView tvSlideTag, tvSlideCategory, tvSlideHeadline, tvSlideDescription;
    private TextView btnNextSlide, btnTopSignIn, btnFastDemoProsumer;
    private View dot0, dot1;
    private Button btnGetStarted, btnSignIn;
    private View layoutAnimatedBrand, frameLogoEmblem;

    private int currentSlide = 0;

    private final String[] tags = {"01 / 02", "02 / 02"};
    private final String[] categories = {"Decentralized Trading", "Instant QR Dispatch"};
    private final String[] headlines = {
            "Trade solar power across local microgrids.",
            "Seamless verification and zero-touch dispatch."
    };
    private final String[] descriptions = {
            "Reserve clean generation dispatch slots, balance grid loads, and access real-time node capacity directly from your mobile device.",
            "Clear your scheduled reservations in real time. Validate energy injection seamlessly at microgrid substations with secure QR dispatch passes."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ApiClient with Application Context
        ApiClient.init(this);

        // If already logged in, navigate straight to Dashboard
        if (SessionManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        tvSlideTag = findViewById(R.id.tvSlideTag);
        tvSlideCategory = findViewById(R.id.tvSlideCategory);
        tvSlideHeadline = findViewById(R.id.tvSlideHeadline);
        tvSlideDescription = findViewById(R.id.tvSlideDescription);
        btnNextSlide = findViewById(R.id.btnNextSlide);
        btnTopSignIn = findViewById(R.id.btnTopSignIn);
        dot0 = findViewById(R.id.dot0);
        dot1 = findViewById(R.id.dot1);
        btnGetStarted = findViewById(R.id.btnGetStarted);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnFastDemoProsumer = findViewById(R.id.btnFastDemoProsumer);
        layoutAnimatedBrand = findViewById(R.id.layoutAnimatedBrand);
        frameLogoEmblem = findViewById(R.id.frameLogoEmblem);

        // Animate the Solarrays emblem into view smoothly
        playBrandEntranceAnimation();

        updateSlideUI();

        btnNextSlide.setOnClickListener(v -> {
            currentSlide = (currentSlide + 1) % 2;
            updateSlideUI();
        });

        dot0.setOnClickListener(v -> {
            currentSlide = 0;
            updateSlideUI();
        });

        dot1.setOnClickListener(v -> {
            currentSlide = 1;
            updateSlideUI();
        });

        btnGetStarted.setOnClickListener(v -> {
            startActivity(new Intent(OnboardingActivity.this, RegisterActivity.class));
        });

        View.OnClickListener goToLogin = v -> {
            startActivity(new Intent(OnboardingActivity.this, LoginActivity.class));
        };

        btnSignIn.setOnClickListener(goToLogin);
        btnTopSignIn.setOnClickListener(goToLogin);

        // 1-Tap Fast Test Login for Prosumer role
        if (btnFastDemoProsumer != null) {
            btnFastDemoProsumer.setOnClickListener(v -> performFastProsumerLogin());
        }
    }

    private void playBrandEntranceAnimation() {
        if (layoutAnimatedBrand != null && frameLogoEmblem != null) {
            layoutAnimatedBrand.setAlpha(0f);
            layoutAnimatedBrand.setTranslationY(-40f);
            frameLogoEmblem.setScaleX(0.35f);
            frameLogoEmblem.setScaleY(0.35f);

            layoutAnimatedBrand.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            frameLogoEmblem.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(1000)
                    .setInterpolator(new OvershootInterpolator(1.7f))
                    .start();
        }
    }

    private void performFastProsumerLogin() {
        Toast.makeText(this, "☀️ Connecting as SunPower Solar Prosumer...", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(OnboardingActivity.this, "✓ Welcome, " + (authUser != null ? authUser.getDisplayName() : "Prosumer") + "!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Toast.makeText(OnboardingActivity.this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(OnboardingActivity.this, "Authentication failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateSlideUI() {
        tvSlideTag.setText(tags[currentSlide]);
        tvSlideCategory.setText(categories[currentSlide]);
        tvSlideHeadline.setText(headlines[currentSlide]);
        tvSlideDescription.setText(descriptions[currentSlide]);

        if (currentSlide == 0) {
            dot0.setBackgroundResource(R.drawable.bg_tab_selected);
            dot0.getLayoutParams().width = (int) (28 * getResources().getDisplayMetrics().density);
            dot0.requestLayout();

            dot1.setBackgroundResource(R.drawable.bg_dark_input);
            dot1.getLayoutParams().width = (int) (8 * getResources().getDisplayMetrics().density);
            dot1.requestLayout();

            btnNextSlide.setText("Next →");
        } else {
            dot0.setBackgroundResource(R.drawable.bg_dark_input);
            dot0.getLayoutParams().width = (int) (8 * getResources().getDisplayMetrics().density);
            dot0.requestLayout();

            dot1.setBackgroundResource(R.drawable.bg_tab_selected);
            dot1.getLayoutParams().width = (int) (28 * getResources().getDisplayMetrics().density);
            dot1.requestLayout();

            btnNextSlide.setText("Previous ←");
        }
    }
}
