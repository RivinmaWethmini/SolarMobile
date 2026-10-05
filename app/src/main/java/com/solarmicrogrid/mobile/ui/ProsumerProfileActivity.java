package com.solarmicrogrid.mobile.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.auth.SessionManager;
import com.solarmicrogrid.mobile.models.AuthUser;
import com.solarmicrogrid.mobile.network.ApiClient;

import org.json.JSONObject;

import java.util.Locale;

/**
 * Modern Solarrays Prosumer Solar Profile Activity.
 * Gracefully displays prosumer telemetry, handles Admin/Operator preview modes,
 * and allows instant hardware editing or deactivation requests.
 */
public class ProsumerProfileActivity extends AppCompatActivity {

    private ImageView btnProfileBack;
    private View layoutRoleNotice;
    private TextView tvNoticeTitle, tvNoticeMessage;
    private Button btnNoticeAction;

    private TextView tvNic;
    private TextView tvName;
    private TextView tvSolarCapacity;
    private TextView tvBatteryCapacity;
    private TextView tvAvailableEnergy;
    private TextView tvPrice;
    private TextView tvLocation;
    private TextView tvStatus;

    private Button btnEditProfile;
    private Button btnDeactivateProfile;

    private String activeNic = "200224700740";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prosumer_profile);

        btnProfileBack = findViewById(R.id.btnProfileBack);
        layoutRoleNotice = findViewById(R.id.layoutRoleNotice);
        tvNoticeTitle = findViewById(R.id.tvNoticeTitle);
        tvNoticeMessage = findViewById(R.id.tvNoticeMessage);
        btnNoticeAction = findViewById(R.id.btnNoticeAction);

        tvNic = findViewById(R.id.tvNic);
        tvName = findViewById(R.id.tvName);
        tvSolarCapacity = findViewById(R.id.tvSolarCapacity);
        tvBatteryCapacity = findViewById(R.id.tvBatteryCapacity);
        tvAvailableEnergy = findViewById(R.id.tvAvailableEnergy);
        tvPrice = findViewById(R.id.tvPrice);
        tvLocation = findViewById(R.id.tvLocation);
        tvStatus = findViewById(R.id.tvStatus);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnDeactivateProfile = findViewById(R.id.btnDeactivateProfile);

        if (btnProfileBack != null) {
            btnProfileBack.setOnClickListener(v -> finish());
        }

        determineNicAndLoadProfile();

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProsumerProfileActivity.this, EditProsumerActivity.class);
            // Save active NIC in legacy pref so Edit screen uses the exact profile
            getSharedPreferences("solar_session", MODE_PRIVATE).edit().putString("prosumer_nic", activeNic).apply();
            startActivity(intent);
        });

        btnDeactivateProfile.setOnClickListener(v -> showDeactivateConfirmation());
    }

    @Override
    protected void onResume() {
        super.onResume();
        determineNicAndLoadProfile();
    }

    private void determineNicAndLoadProfile() {
        AuthUser user = SessionManager.getInstance(this).getUser();

        if (user != null && !user.isProsumer()) {
            // Logged in as Admin, Backoffice, or Operator
            activeNic = "200224700740"; // Active seeded Prosumer
            if (layoutRoleNotice != null) {
                layoutRoleNotice.setVisibility(View.VISIBLE);
                tvNoticeTitle.setText("ℹ️ " + user.getRole() + " View");
                tvNoticeMessage.setText("Logged in as " + user.getDisplayName() + ". Showing live registered Prosumer profile for SunPower Station A (NIC: 200224700740).");
                btnNoticeAction.setText("Switch to Solar Prosumer Account");
                btnNoticeAction.setOnClickListener(v -> switchToProsumer());
            }
        } else if (user != null && user.getNic() != null && !user.getNic().trim().isEmpty()) {
            activeNic = user.getNic();
            if (layoutRoleNotice != null) {
                layoutRoleNotice.setVisibility(View.GONE);
            }
        } else {
            activeNic = "200224700740";
        }

        loadProsumerData(activeNic);
    }

    private void loadProsumerData(String nic) {
        tvName.setText("Connecting to grid API...");
        ApiClient.get("/Prosumer/" + nic, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject prosumer = new JSONObject(response);
                    tvNic.setText("NIC: " + prosumer.optString("nic", nic));
                    tvName.setText(prosumer.optString("name", "Solar Station"));
                    tvSolarCapacity.setText(prosumer.optDouble("solarCapacityKw", 0) + " kW");
                    tvBatteryCapacity.setText(prosumer.optDouble("batteryCapacityKwh", 0) + " kWh");
                    tvAvailableEnergy.setText("Available: " + prosumer.optDouble("availableEnergyKw", 0) + " kW");
                    tvPrice.setText("LKR " + String.format(Locale.US, "%.2f", prosumer.optDouble("pricePerKwh", 0)) + " / kWh");
                    tvLocation.setText("Station: " + prosumer.optString("location", "Grid Node"));

                    boolean available = prosumer.optBoolean("isAvailable", true);
                    if (available) {
                        tvStatus.setText("Available");
                        tvStatus.setTextColor(getColor(R.color.emerald_approved));
                    } else {
                        tvStatus.setText("Deactivated");
                        tvStatus.setTextColor(getColor(R.color.red_rejected));
                    }
                } catch (Exception e) {
                    tvName.setText("Parse error: " + e.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                // If 404 and user is logged in, offer to initialize or fallback
                if (errorMessage.contains("404") || errorMessage.toLowerCase(Locale.US).contains("not found")) {
                    if (!activeNic.equals("200224700740")) {
                        // Fallback to active demo prosumer
                        activeNic = "200224700740";
                        if (layoutRoleNotice != null) {
                            layoutRoleNotice.setVisibility(View.VISIBLE);
                            tvNoticeTitle.setText("Prosumer Record Preview");
                            tvNoticeMessage.setText("Your NIC is not in the Prosumer registry yet. Displaying active registered station SunPower Station A (NIC: 200224700740).");
                            btnNoticeAction.setText("Auto-Register My Prosumer Profile");
                            btnNoticeAction.setOnClickListener(v -> createDefaultProsumerProfile());
                        }
                        loadProsumerData(activeNic);
                    } else {
                        tvName.setText("Prosumer Record Not Found");
                        Toast.makeText(ProsumerProfileActivity.this, "Grid API: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    tvName.setText("Offline / Network Error");
                    Toast.makeText(ProsumerProfileActivity.this, "Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void switchToProsumer() {
        Toast.makeText(this, "☀️ Switching to SunPower Prosumer...", Toast.LENGTH_SHORT).show();
        ApiClient.login("prosumer@solar.com", "Prosumer@12345", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    String accessToken = json.optString("accessToken", "");
                    String refreshToken = json.optString("refreshToken", "");
                    JSONObject userObj = json.optJSONObject("user");
                    AuthUser authUser = userObj != null ? AuthUser.fromJson(userObj) : null;

                    SessionManager.getInstance(ProsumerProfileActivity.this).saveSession(accessToken, refreshToken, authUser);
                    determineNicAndLoadProfile();
                    Toast.makeText(ProsumerProfileActivity.this, "✓ Switched to Prosumer: " + (authUser != null ? authUser.getDisplayName() : "SunPower"), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(ProsumerProfileActivity.this, "Switch failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ProsumerProfileActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createDefaultProsumerProfile() {
        AuthUser user = SessionManager.getInstance(this).getUser();
        String userNic = (user != null && user.getNic() != null && !user.getNic().isEmpty()) ? user.getNic() : "200224700740";
        String userName = (user != null && user.getFullName() != null) ? user.getFullName() : "Solar Station Participant";

        try {
            JSONObject body = new JSONObject();
            body.put("nic", userNic);
            body.put("name", userName);
            body.put("solarCapacityKw", 15.0);
            body.put("batteryCapacityKwh", 30.0);
            body.put("pricePerKwh", 42.0);
            body.put("location", "Western Grid Substation Node");
            body.put("isAvailable", true);

            ApiClient.post("/Prosumer", body.toString(), new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Toast.makeText(ProsumerProfileActivity.this, "✓ Solar hardware profile created!", Toast.LENGTH_SHORT).show();
                    activeNic = userNic;
                    if (layoutRoleNotice != null) layoutRoleNotice.setVisibility(View.GONE);
                    loadProsumerData(activeNic);
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(ProsumerProfileActivity.this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error building request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeactivateConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Request Deactivation")
                .setMessage("Are you sure you want to deactivate your prosumer account? Only Backoffice administrators will be able to reactivate it.")
                .setPositiveButton("Deactivate", (dialog, which) -> deactivateProfile())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deactivateProfile() {
        ApiClient.patch("/Prosumer/" + activeNic + "/deactivate", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                Toast.makeText(ProsumerProfileActivity.this, "Prosumer account deactivated successfully", Toast.LENGTH_LONG).show();
                loadProsumerData(activeNic);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ProsumerProfileActivity.this, "Failed to deactivate: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}