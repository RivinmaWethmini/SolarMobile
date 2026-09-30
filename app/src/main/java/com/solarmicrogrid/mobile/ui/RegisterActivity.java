package com.solarmicrogrid.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
 * Mobile Registration Activity supporting a 2-step registration process:
 * Step 1: Participant details, credentials, and role selection (Prosumer / Consumer).
 * Step 2: 6-Digit Email verification passcode.
 * Matches Register.jsx from the Web frontend.
 */
public class RegisterActivity extends AppCompatActivity {

    private ImageView btnRegisterBack;
    private TextView tvStepIndicator, tvRegisterTitle, tvRegisterSubtitle;
    private ProgressBar pbRegisterLoading;
    private TextView btnGoToLogin;

    // Step 1 Views
    private LinearLayout layoutRegisterStep1;
    private EditText etFullName, etUsername, etEmail, etNic;
    private LinearLayout cardRoleProsumer, cardRoleConsumer;
    private TextView tvRoleProsumerTitle, tvRoleConsumerTitle;
    private EditText etRegPassword, etConfirmPassword;
    private CheckBox cbTerms;
    private Button btnContinueToOtp;

    private String selectedRole = "Prosumer"; // Default to Prosumer

    // Step 2 Views
    private LinearLayout layoutRegisterStep2;
    private TextView tvRegMaskedEmail, btnBackToStep1, tvRegResendTimer;
    private EditText[] regOtpDigits = new EditText[6];
    private Button btnSubmitRegistration;
    private CountDownTimer countDownTimer;
    private boolean isOtpTimerRunning = false;

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

        setContentView(R.layout.activity_register);

        initViews();
        setupRoleSelection();
        setupStep1();
        setupStep2();

        btnRegisterBack.setOnClickListener(v -> finish());
        btnGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void initViews() {
        btnRegisterBack = findViewById(R.id.btnRegisterBack);
        tvStepIndicator = findViewById(R.id.tvStepIndicator);
        tvRegisterTitle = findViewById(R.id.tvRegisterTitle);
        tvRegisterSubtitle = findViewById(R.id.tvRegisterSubtitle);
        pbRegisterLoading = findViewById(R.id.pbRegisterLoading);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);

        // Step 1
        layoutRegisterStep1 = findViewById(R.id.layoutRegisterStep1);
        etFullName = findViewById(R.id.etFullName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etNic = findViewById(R.id.etNic);
        cardRoleProsumer = findViewById(R.id.cardRoleProsumer);
        cardRoleConsumer = findViewById(R.id.cardRoleConsumer);
        tvRoleProsumerTitle = findViewById(R.id.tvRoleProsumerTitle);
        tvRoleConsumerTitle = findViewById(R.id.tvRoleConsumerTitle);
        etRegPassword = findViewById(R.id.etRegPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnContinueToOtp = findViewById(R.id.btnContinueToOtp);

        // Step 2
        layoutRegisterStep2 = findViewById(R.id.layoutRegisterStep2);
        tvRegMaskedEmail = findViewById(R.id.tvRegMaskedEmail);
        btnBackToStep1 = findViewById(R.id.btnBackToStep1);
        tvRegResendTimer = findViewById(R.id.tvRegResendTimer);
        btnSubmitRegistration = findViewById(R.id.btnSubmitRegistration);

        regOtpDigits[0] = findViewById(R.id.regOtpDigit1);
        regOtpDigits[1] = findViewById(R.id.regOtpDigit2);
        regOtpDigits[2] = findViewById(R.id.regOtpDigit3);
        regOtpDigits[3] = findViewById(R.id.regOtpDigit4);
        regOtpDigits[4] = findViewById(R.id.regOtpDigit5);
        regOtpDigits[5] = findViewById(R.id.regOtpDigit6);
    }

    private void setupRoleSelection() {
        updateRoleCards();

        cardRoleProsumer.setOnClickListener(v -> {
            selectedRole = "Prosumer";
            updateRoleCards();
        });

        cardRoleConsumer.setOnClickListener(v -> {
            selectedRole = "Consumer";
            updateRoleCards();
        });
    }

    private void updateRoleCards() {
        if ("Prosumer".equalsIgnoreCase(selectedRole)) {
            cardRoleProsumer.setBackgroundResource(R.drawable.bg_dark_input);
            cardRoleProsumer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0x33FFD000));
            tvRoleProsumerTitle.setTextColor(getColor(R.color.yellow_primary));

            cardRoleConsumer.setBackgroundResource(R.drawable.bg_dark_input);
            cardRoleConsumer.setBackgroundTintList(null);
            tvRoleConsumerTitle.setTextColor(getColor(R.color.text_white));
        } else {
            cardRoleConsumer.setBackgroundResource(R.drawable.bg_dark_input);
            cardRoleConsumer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0x3310B981));
            tvRoleConsumerTitle.setTextColor(getColor(R.color.emerald_approved));

            cardRoleProsumer.setBackgroundResource(R.drawable.bg_dark_input);
            cardRoleProsumer.setBackgroundTintList(null);
            tvRoleProsumerTitle.setTextColor(getColor(R.color.text_white));
        }
    }

    private void setupStep1() {
        btnContinueToOtp.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String nic = etNic.getText().toString().trim();
            String password = etRegPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();

            if (fullName.isEmpty()) {
                etFullName.setError("Please enter your full name or organization");
                etFullName.requestFocus();
                return;
            }

            if (username.isEmpty()) {
                etUsername.setError("Choose a username for login");
                etUsername.requestFocus();
                return;
            }

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email address");
                etEmail.requestFocus();
                return;
            }

            if (nic.isEmpty()) {
                etNic.setError("Enter your NIC or Solar Account ID");
                etNic.requestFocus();
                return;
            }

            if (password.length() < 6) {
                etRegPassword.setError("Password must be at least 6 characters");
                etRegPassword.requestFocus();
                return;
            }

            if (!password.equals(confirmPassword)) {
                etConfirmPassword.setError("Passwords do not match");
                etConfirmPassword.requestFocus();
                return;
            }

            if (!cbTerms.isChecked()) {
                Toast.makeText(this, "Please accept the Grid Interconnection Terms", Toast.LENGTH_SHORT).show();
                return;
            }

            dispatchRegistrationOtp(email, selectedRole);
        });
    }

    private void dispatchRegistrationOtp(String email, String role) {
        showLoading(true);
        ApiClient.sendRegistrationOtp(email, role, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                showLoading(false);
                try {
                    JSONObject json = new JSONObject(response);
                    String masked = json.optString("maskedEmail", email);
                    tvRegMaskedEmail.setText("Passcode sent to: " + masked);

                    layoutRegisterStep1.setVisibility(View.GONE);
                    layoutRegisterStep2.setVisibility(View.VISIBLE);

                    tvStepIndicator.setText("Step 2 of 2: Email Verification");
                    tvRegisterTitle.setText("Verify email");
                    tvRegisterSubtitle.setText("Enter the 6-digit passcode sent to your address to activate your account.");

                    startOtpTimer();
                    regOtpDigits[0].requestFocus();
                    Toast.makeText(RegisterActivity.this, "Passcode sent to " + masked, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(RegisterActivity.this, "Passcode sent to your email", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupStep2() {
        btnBackToStep1.setOnClickListener(v -> {
            layoutRegisterStep2.setVisibility(View.GONE);
            layoutRegisterStep1.setVisibility(View.VISIBLE);
            tvStepIndicator.setText("Step 1 of 2: Participant Details");
            tvRegisterTitle.setText("Join Solis grid");
            tvRegisterSubtitle.setText("Trade solar energy, reserve dispatch passes, and track grid capacity.");
            if (countDownTimer != null) countDownTimer.cancel();
            isOtpTimerRunning = false;
        });

        setupOtpInputJumps();

        btnSubmitRegistration.setOnClickListener(v -> {
            String code = getEnteredOtp();
            if (code.length() != 6) {
                Toast.makeText(this, "Please enter all 6 digits of the passcode", Toast.LENGTH_SHORT).show();
                return;
            }
            executeAccountRegistration(code);
        });
    }

    private void setupOtpInputJumps() {
        for (int i = 0; i < 6; i++) {
            final int index = i;
            regOtpDigits[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < 5) {
                        regOtpDigits[index + 1].requestFocus();
                    }
                    if (getEnteredOtp().length() == 6) {
                        btnSubmitRegistration.performClick();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            regOtpDigits[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (regOtpDigits[index].getText().toString().isEmpty() && index > 0) {
                        regOtpDigits[index - 1].requestFocus();
                        regOtpDigits[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private String getEnteredOtp() {
        StringBuilder sb = new StringBuilder();
        for (EditText et : regOtpDigits) {
            sb.append(et.getText().toString().trim());
        }
        return sb.toString();
    }

    private void startOtpTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isOtpTimerRunning = true;
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvRegResendTimer.setText("Resend passcode in " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                isOtpTimerRunning = false;
                tvRegResendTimer.setText("Resend Passcode");
                tvRegResendTimer.setOnClickListener(v -> {
                    if (!isOtpTimerRunning) {
                        dispatchRegistrationOtp(etEmail.getText().toString().trim(), selectedRole);
                    }
                });
            }
        }.start();
    }

    private void executeAccountRegistration(String otpCode) {
        showLoading(true);
        try {
            JSONObject payload = new JSONObject();
            payload.put("fullName", etFullName.getText().toString().trim());
            payload.put("username", etUsername.getText().toString().trim());
            payload.put("email", etEmail.getText().toString().trim().toLowerCase());
            payload.put("nic", etNic.getText().toString().trim());
            payload.put("role", selectedRole);
            payload.put("password", etRegPassword.getText().toString());
            payload.put("otp", otpCode);
            payload.put("deviceInfo", "SolarMobile (" + etFullName.getText().toString().trim() + ")");

            ApiClient.register(payload, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    showLoading(false);
                    try {
                        JSONObject res = new JSONObject(response);
                        String accessToken = res.optString("accessToken", "");
                        String refreshToken = res.optString("refreshToken", "");
                        JSONObject userJson = res.optJSONObject("user");

                        AuthUser authUser = AuthUser.fromJson(userJson);
                        SessionManager.getInstance(RegisterActivity.this).saveSession(accessToken, refreshToken, authUser);

                        if (authUser != null && authUser.isPendingApproval()) {
                            Toast.makeText(RegisterActivity.this, "Registration verified! Prosumer accounts require Operator verification before energy injection.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegisterActivity.this, "☀️ Welcome to Solis Microgrid, " + (authUser != null ? authUser.getDisplayName() : "Participant") + "!", Toast.LENGTH_LONG).show();
                        }

                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(RegisterActivity.this, "Session parsing error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    showLoading(false);
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            showLoading(false);
            Toast.makeText(this, "Failed to build registration payload: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean loading) {
        pbRegisterLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnContinueToOtp.setEnabled(!loading);
        btnSubmitRegistration.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
