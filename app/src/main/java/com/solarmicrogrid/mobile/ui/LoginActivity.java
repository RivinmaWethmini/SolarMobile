package com.solarmicrogrid.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.auth.SessionManager;
import com.solarmicrogrid.mobile.models.AuthUser;
import com.solarmicrogrid.mobile.network.ApiClient;

import org.json.JSONObject;

/**
 * Mobile Login Activity supporting dual authentication mechanisms:
 * 1. Password Login (Email/Username + Password)
 * 2. 6-Digit Email Passcode (OTP) with countdown cooldown
 * Matches Login.jsx from the Web frontend.
 */
public class LoginActivity extends AppCompatActivity {

    private TextView btnBack;
    private TextView tabPassword, tabOtp;
    private LinearLayout layoutPasswordForm, layoutOtpForm;
    private ProgressBar pbLoading;

    // Password Form Views
    private EditText etIdentifier, etPassword;
    private TextView btnTogglePassword;
    private Button btnSubmitPasswordLogin;
    private boolean isPasswordVisible = false;

    // OTP Form Views
    private LinearLayout layoutOtpStep1, layoutOtpStep2;
    private EditText etOtpIdentifier;
    private Button btnSendOtp, btnVerifyOtp;
    private TextView tvMaskedEmail, btnChangeOtpEmail, tvResendTimer;
    private EditText[] otpDigits = new EditText[6];
    private CountDownTimer countDownTimer;
    private boolean isOtpTimerRunning = false;

    private TextView btnGoToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApiClient.init(this);

        // If already logged in, navigate straight to Dashboard
        if (SessionManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        initViews();
        setupTabSwitching();
        setupPasswordForm();
        setupOtpForm();

        btnBack.setOnClickListener(v -> finish());
        btnGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tabPassword = findViewById(R.id.tabPassword);
        tabOtp = findViewById(R.id.tabOtp);
        layoutPasswordForm = findViewById(R.id.layoutPasswordForm);
        layoutOtpForm = findViewById(R.id.layoutOtpForm);
        pbLoading = findViewById(R.id.pbLoading);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);

        // Password Form
        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnSubmitPasswordLogin = findViewById(R.id.btnSubmitPasswordLogin);

        // OTP Form
        layoutOtpStep1 = findViewById(R.id.layoutOtpStep1);
        layoutOtpStep2 = findViewById(R.id.layoutOtpStep2);
        etOtpIdentifier = findViewById(R.id.etOtpIdentifier);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        tvMaskedEmail = findViewById(R.id.tvMaskedEmail);
        btnChangeOtpEmail = findViewById(R.id.btnChangeOtpEmail);
        tvResendTimer = findViewById(R.id.tvResendTimer);

        otpDigits[0] = findViewById(R.id.otpDigit1);
        otpDigits[1] = findViewById(R.id.otpDigit2);
        otpDigits[2] = findViewById(R.id.otpDigit3);
        otpDigits[3] = findViewById(R.id.otpDigit4);
        otpDigits[4] = findViewById(R.id.otpDigit5);
        otpDigits[5] = findViewById(R.id.otpDigit6);
    }

    private void setupTabSwitching() {
        tabPassword.setOnClickListener(v -> {
            tabPassword.setBackgroundResource(R.drawable.bg_tab_selected);
            tabPassword.setTextColor(getColor(R.color.bg_black));

            tabOtp.setBackgroundResource(R.drawable.bg_tab_unselected);
            tabOtp.setTextColor(getColor(R.color.text_light_secondary));

            layoutPasswordForm.setVisibility(View.VISIBLE);
            layoutOtpForm.setVisibility(View.GONE);
        });

        tabOtp.setOnClickListener(v -> {
            tabOtp.setBackgroundResource(R.drawable.bg_tab_selected);
            tabOtp.setTextColor(getColor(R.color.bg_black));

            tabPassword.setBackgroundResource(R.drawable.bg_tab_unselected);
            tabPassword.setTextColor(getColor(R.color.text_light_secondary));

            layoutPasswordForm.setVisibility(View.GONE);
            layoutOtpForm.setVisibility(View.VISIBLE);
        });
    }

    private void setupPasswordForm() {
        btnTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnTogglePassword.setText("Show");
                isPasswordVisible = false;
            } else {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                btnTogglePassword.setText("Hide");
                isPasswordVisible = true;
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        btnSubmitPasswordLogin.setOnClickListener(v -> {
            String identifier = etIdentifier.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (identifier.isEmpty()) {
                etIdentifier.setError("Please enter your email or username");
                etIdentifier.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                etPassword.setError("Please enter your password");
                etPassword.requestFocus();
                return;
            }

            performPasswordLogin(identifier, password);
        });
    }

    private void performPasswordLogin(String identifier, String password) {
        showLoading(true);
        ApiClient.login(identifier, password, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                showLoading(false);
                handleAuthSuccess(response);
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupOtpForm() {
        btnSendOtp.setOnClickListener(v -> {
            String identifier = etOtpIdentifier.getText().toString().trim();
            if (identifier.isEmpty()) {
                etOtpIdentifier.setError("Enter registered username or email");
                etOtpIdentifier.requestFocus();
                return;
            }
            requestLoginOtp(identifier);
        });

        btnChangeOtpEmail.setOnClickListener(v -> {
            layoutOtpStep2.setVisibility(View.GONE);
            layoutOtpStep1.setVisibility(View.VISIBLE);
            if (countDownTimer != null) countDownTimer.cancel();
            isOtpTimerRunning = false;
        });

        setupOtpInputJumps();

        btnVerifyOtp.setOnClickListener(v -> {
            String code = getEnteredOtp();
            if (code.length() != 6) {
                Toast.makeText(this, "Please enter all 6 digits of the passcode", Toast.LENGTH_SHORT).show();
                return;
            }
            String identifier = etOtpIdentifier.getText().toString().trim();
            verifyLoginOtp(identifier, code);
        });
    }

    private void setupOtpInputJumps() {
        for (int i = 0; i < 6; i++) {
            final int index = i;
            otpDigits[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < 5) {
                        otpDigits[index + 1].requestFocus();
                    }
                    if (getEnteredOtp().length() == 6) {
                        btnVerifyOtp.performClick();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            otpDigits[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (otpDigits[index].getText().toString().isEmpty() && index > 0) {
                        otpDigits[index - 1].requestFocus();
                        otpDigits[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private String getEnteredOtp() {
        StringBuilder sb = new StringBuilder();
        for (EditText et : otpDigits) {
            sb.append(et.getText().toString().trim());
        }
        return sb.toString();
    }

    private void requestLoginOtp(String identifier) {
        showLoading(true);
        ApiClient.sendLoginOtp(identifier, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                showLoading(false);
                try {
                    JSONObject json = new JSONObject(response);
                    String masked = json.optString("maskedEmail", "your registered email");
                    tvMaskedEmail.setText("Sent to: " + masked);

                    layoutOtpStep1.setVisibility(View.GONE);
                    layoutOtpStep2.setVisibility(View.VISIBLE);

                    startOtpTimer();
                    otpDigits[0].requestFocus();
                    Toast.makeText(LoginActivity.this, "Passcode sent to " + masked, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "Passcode sent to your registered email", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void verifyLoginOtp(String identifier, String code) {
        showLoading(true);
        ApiClient.verifyOtp(identifier, code, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                showLoading(false);
                handleAuthSuccess(response);
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startOtpTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isOtpTimerRunning = true;
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvResendTimer.setText("Resend passcode in " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                isOtpTimerRunning = false;
                tvResendTimer.setText("Resend Passcode");
                tvResendTimer.setOnClickListener(v -> {
                    if (!isOtpTimerRunning) {
                        requestLoginOtp(etOtpIdentifier.getText().toString().trim());
                    }
                });
            }
        }.start();
    }

    private void handleAuthSuccess(String jsonResponse) {
        try {
            JSONObject res = new JSONObject(jsonResponse);
            String accessToken = res.optString("accessToken", "");
            String refreshToken = res.optString("refreshToken", "");
            JSONObject userJson = res.optJSONObject("user");

            AuthUser authUser = AuthUser.fromJson(userJson);
            SessionManager.getInstance(this).saveSession(accessToken, refreshToken, authUser);

            String greeting = authUser != null ? authUser.getDisplayName() : "Participant";
            Toast.makeText(this, "Welcome back, " + greeting + "!", Toast.LENGTH_LONG).show();

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Session parsing error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showLoading(boolean loading) {
        pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmitPasswordLogin.setEnabled(!loading);
        btnSendOtp.setEnabled(!loading);
        btnVerifyOtp.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
