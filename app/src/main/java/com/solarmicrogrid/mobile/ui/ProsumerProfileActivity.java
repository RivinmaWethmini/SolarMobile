package com.solarmicrogrid.mobile.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.network.ApiClient;

import org.json.JSONObject;

public class ProsumerProfileActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prosumer_profile);

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

        loadProsumerProfile();

        // Edit Profile button
        btnEditProfile.setOnClickListener(v -> {

            Intent intent = new Intent(
                    ProsumerProfileActivity.this,
                    EditProsumerActivity.class
            );

            startActivity(intent);
        });

        // Request Deactivation button
        btnDeactivateProfile.setOnClickListener(v -> {
            showDeactivateConfirmation();
        });
    }

    private void loadProsumerProfile() {

        SharedPreferences prefs =
                getSharedPreferences("solar_session", MODE_PRIVATE);

        String nic = prefs.getString(
                "prosumer_nic",
                "200224700740"
        );

        if (nic == null || nic.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Prosumer NIC not found",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        ApiClient.get(
                "/Prosumer/" + nic,
                new ApiClient.ApiCallback() {

                    @Override
                    public void onSuccess(String response) {

                        try {

                            JSONObject prosumer =
                                    new JSONObject(response);

                            tvNic.setText(
                                    "NIC: " +
                                    prosumer.optString("nic")
                            );

                            tvName.setText(
                                    "Name: " +
                                    prosumer.optString("name")
                            );

                            tvSolarCapacity.setText(
                                    "Solar Capacity: " +
                                    prosumer.optDouble(
                                            "solarCapacityKw",
                                            0
                                    ) +
                                    " kW"
                            );

                            tvBatteryCapacity.setText(
                                    "Battery Capacity: " +
                                    prosumer.optDouble(
                                            "batteryCapacityKwh",
                                            0
                                    ) +
                                    " kWh"
                            );

                            tvAvailableEnergy.setText(
                                    "Available Energy: " +
                                    prosumer.optDouble(
                                            "availableEnergyKw",
                                            0
                                    ) +
                                    " kW"
                            );

                            tvPrice.setText(
                                    "Price: Rs. " +
                                    prosumer.optDouble(
                                            "pricePerKwh",
                                            0
                                    ) +
                                    " / kWh"
                            );

                            tvLocation.setText(
                                    "Location: " +
                                    prosumer.optString(
                                            "location",
                                            "-"
                                    )
                            );

                            boolean isAvailable =
                                    prosumer.optBoolean(
                                            "isAvailable",
                                            false
                                    );

                            if (isAvailable) {

                                tvStatus.setText(
                                        "Status: Available"
                                );

                                btnEditProfile.setEnabled(true);
                                btnDeactivateProfile.setEnabled(true);

                            } else {

                                tvStatus.setText(
                                        "Status: Deactivated"
                                );

                                btnEditProfile.setEnabled(false);
                                btnDeactivateProfile.setEnabled(false);
                            }

                        } catch (Exception e) {

                            Toast.makeText(
                                    ProsumerProfileActivity.this,
                                    "Invalid profile response",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {

                        Toast.makeText(
                                ProsumerProfileActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void showDeactivateConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("Request Deactivation")
                .setMessage(
                        "Are you sure you want to request deactivation of your prosumer account?"
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Confirm",
                        (dialog, which) -> {
                            deactivateProsumer();
                        }
                )
                .show();
    }

    private void deactivateProsumer() {

        SharedPreferences prefs =
                getSharedPreferences("solar_session", MODE_PRIVATE);

        String nic = prefs.getString(
                "prosumer_nic",
                "200224700740"
        );

        ApiClient.patch(
                "/Prosumer/" + nic + "/deactivate",
                "",
                new ApiClient.ApiCallback() {

                    @Override
                    public void onSuccess(String response) {

                        Toast.makeText(
                                ProsumerProfileActivity.this,
                                "Prosumer account deactivated",
                                Toast.LENGTH_LONG
                        ).show();

                        loadProsumerProfile();
                    }

                    @Override
                    public void onError(String errorMessage) {

                        Toast.makeText(
                                ProsumerProfileActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }
}