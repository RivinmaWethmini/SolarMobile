package com.solarmicrogrid.mobile.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.network.ApiClient;

import org.json.JSONObject;

public class EditProsumerActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etSolarCapacity;
    private EditText etBatteryCapacity;
    private EditText etAvailableEnergy;
    private EditText etPrice;
    private EditText etLocation;

    private Button btnSaveProfile;

    private String nic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_prosumer);

        etName = findViewById(R.id.etName);
        etSolarCapacity = findViewById(R.id.etSolarCapacity);
        etBatteryCapacity = findViewById(R.id.etBatteryCapacity);
        etAvailableEnergy = findViewById(R.id.etAvailableEnergy);
        etPrice = findViewById(R.id.etPrice);
        etLocation = findViewById(R.id.etLocation);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        SharedPreferences prefs =
                getSharedPreferences("solar_session", MODE_PRIVATE);

        nic = prefs.getString("prosumer_nic", "200224700740");

        loadProsumer();

        btnSaveProfile.setOnClickListener(v -> updateProsumer());
    }

    private void loadProsumer() {

        ApiClient.get("/Prosumer/" + nic, new ApiClient.ApiCallback() {

            @Override
            public void onSuccess(String response) {

                try {
                    JSONObject prosumer = new JSONObject(response);

                    etName.setText(
                            prosumer.optString("name", "")
                    );

                    etSolarCapacity.setText(
                            String.valueOf(
                                    prosumer.optDouble("solarCapacityKw", 0)
                            )
                    );

                    etBatteryCapacity.setText(
                            String.valueOf(
                                    prosumer.optDouble("batteryCapacityKwh", 0)
                            )
                    );

                    etAvailableEnergy.setText(
                            String.valueOf(
                                    prosumer.optDouble("availableEnergyKw", 0)
                            )
                    );

                    etPrice.setText(
                            String.valueOf(
                                    prosumer.optDouble("pricePerKwh", 0)
                            )
                    );

                    etLocation.setText(
                            prosumer.optString("location", "")
                    );

                } catch (Exception e) {

                    Toast.makeText(
                            EditProsumerActivity.this,
                            "Invalid profile response",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onError(String errorMessage) {

                Toast.makeText(
                        EditProsumerActivity.this,
                        errorMessage,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void updateProsumer() {

        try {

            JSONObject payload = new JSONObject();

            payload.put("nic", nic);
            payload.put("name", etName.getText().toString().trim());
            payload.put(
                    "solarCapacityKw",
                    Double.parseDouble(
                            etSolarCapacity.getText().toString().trim()
                    )
            );
            payload.put(
                    "batteryCapacityKwh",
                    Double.parseDouble(
                            etBatteryCapacity.getText().toString().trim()
                    )
            );
            payload.put(
                    "availableEnergyKw",
                    Double.parseDouble(
                            etAvailableEnergy.getText().toString().trim()
                    )
            );
            payload.put(
                    "pricePerKwh",
                    Double.parseDouble(
                            etPrice.getText().toString().trim()
                    )
            );
            payload.put(
                    "location",
                    etLocation.getText().toString().trim()
            );

            ApiClient.put(
                    "/Prosumer/" + nic,
                    payload.toString(),
                    new ApiClient.ApiCallback() {

                        @Override
                        public void onSuccess(String response) {

                            Toast.makeText(
                                    EditProsumerActivity.this,
                                    "Profile updated successfully",
                                    Toast.LENGTH_LONG
                            ).show();

                            finish();
                        }

                        @Override
                        public void onError(String errorMessage) {

                            Toast.makeText(
                                    EditProsumerActivity.this,
                                    errorMessage,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

        } catch (NumberFormatException e) {

            Toast.makeText(
                    this,
                    "Please enter valid numeric values",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to update profile",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}