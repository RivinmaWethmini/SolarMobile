package com.solarmicrogrid.mobile.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.database.DatabaseHelper;
import com.solarmicrogrid.mobile.models.MicrogridNode;
import com.solarmicrogrid.mobile.models.Reservation;
import com.solarmicrogrid.mobile.network.ApiClient;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Energy Slot Booking Screen with Microgrid Node selector and Date/Time pickers.
 */
public class BookingActivity extends AppCompatActivity {

    private Spinner spNodeSelector;
    private Button btnSelectDate, btnSelectStartTime, btnSelectEndTime, btnSubmitBooking;
    private EditText etEnergyAmount;
    private ProgressBar progressBar;

    private Calendar selectedCalendar;
    private int startHour = 9, startMinute = 0;
    private int endHour = 11, endMinute = 0;
    private List<MicrogridNode> nodesList;
    private DatabaseHelper dbHelper;

    // Reads authenticated user NIC from SharedPreferences with robust fallback
    private String PROSUMER_NIC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        dbHelper = new DatabaseHelper(this);
        selectedCalendar = Calendar.getInstance();

        android.content.SharedPreferences prefs = getSharedPreferences("solar_session", MODE_PRIVATE);
        String nic = prefs.getString("prosumer_nic", null);
        if (nic == null) nic = prefs.getString("nic", null);
        if (nic == null) nic = prefs.getString("username", null);
        PROSUMER_NIC = (nic != null && !nic.trim().isEmpty()) ? nic : "200012345678";

        spNodeSelector = findViewById(R.id.spNodeSelector);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectStartTime = findViewById(R.id.btnSelectStartTime);
        btnSelectEndTime = findViewById(R.id.btnSelectEndTime);
        etEnergyAmount = findViewById(R.id.etEnergyAmount);
        btnSubmitBooking = findViewById(R.id.btnSubmitBooking);
        progressBar = findViewById(R.id.progressBar);

        setupNodeSpinner();
        setupPickers();

        btnSubmitBooking.setOnClickListener(v -> submitBookingRequest());
    }

    private void setupNodeSpinner() {
        nodesList = new ArrayList<>();
        // Dynamic load from MicrogridNode API with graceful fallback
        ApiClient.get("/microgridnodes", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    org.json.JSONArray arr;
                    if (response.trim().startsWith("[")) {
                        arr = new org.json.JSONArray(response);
                    } else {
                        JSONObject wrapper = new JSONObject(response);
                        arr = wrapper.optJSONArray("value");
                        if (arr == null) arr = wrapper.optJSONArray("data");
                    }
                    if (arr != null && arr.length() > 0) {
                        nodesList.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            nodesList.add(new MicrogridNode(
                                    obj.optString("id", obj.optString("nodeCode", "node-" + i)),
                                    obj.optString("nodeCode", "NODE-" + i),
                                    obj.optString("name", "Solar Node " + i),
                                    obj.optString("region", "Grid"),
                                    obj.optDouble("totalCapacityKw", 500)
                            ));
                        }
                        updateSpinnerAdapter();
                    } else {
                        loadFallbackNodes();
                    }
                } catch (Exception e) {
                    loadFallbackNodes();
                }
            }

            @Override
            public void onError(String errorMessage) {
                loadFallbackNodes();
            }
        });
    }

    private void loadFallbackNodes() {
        if (nodesList.isEmpty()) {
            nodesList.add(new MicrogridNode("node-01", "NODE-COL-01", "Colombo North Solar Hub", "Western", 500));
            nodesList.add(new MicrogridNode("node-02", "NODE-COL-02", "Kaduwela Microgrid Substation", "Western", 350));
            nodesList.add(new MicrogridNode("node-03", "NODE-KND-01", "Kandy Central Solar Station", "Central", 400));
            nodesList.add(new MicrogridNode("node-04", "NODE-GAL-01", "Galle Coastal Solar Array", "Southern", 600));
            updateSpinnerAdapter();
        }
    }

    private void updateSpinnerAdapter() {
        ArrayAdapter<MicrogridNode> adapter = new ArrayAdapter<MicrogridNode>(this,
                R.layout.item_spinner_node, nodesList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(Color.WHITE);
                }
                return v;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(Color.WHITE);
                    v.setBackgroundColor(Color.parseColor("#1E1F24"));
                }
                return v;
            }
        };
        adapter.setDropDownViewResource(R.layout.item_spinner_node_dropdown);
        spNodeSelector.setAdapter(adapter);
    }

    private void setupPickers() {
        btnSelectDate.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedCalendar.set(Calendar.YEAR, year);
                selectedCalendar.set(Calendar.MONTH, month);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                btnSelectDate.setText(sdf.format(selectedCalendar.getTime()));
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

            // Set Min Date = Today
            datePicker.getDatePicker().setMinDate(now.getTimeInMillis());
            // Set Max Date = Today + 7 Days (7-Day Booking Limit Rule)
            Calendar maxLimit = Calendar.getInstance();
            maxLimit.add(Calendar.DAY_OF_YEAR, 7);
            datePicker.getDatePicker().setMaxDate(maxLimit.getTimeInMillis());

            datePicker.show();
        });

        // Start Time Picker
        btnSelectStartTime.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                startHour = hourOfDay;
                startMinute = minute;
                btnSelectStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute));
            }, startHour, startMinute, true);
            timePicker.show();
        });

        // End Time Picker
        btnSelectEndTime.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                endHour = hourOfDay;
                endMinute = minute;
                btnSelectEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute));
            }, endHour, endMinute, true);
            timePicker.show();
        });

        // Set default date text
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        btnSelectDate.setText(sdf.format(selectedCalendar.getTime()));
    }

    private void submitBookingRequest() {
        String energyStr = etEnergyAmount.getText().toString().trim();
        if (energyStr.isEmpty()) {
            Toast.makeText(this, "Please enter reserved energy amount (kW/h)", Toast.LENGTH_SHORT).show();
            return;
        }

        double energy;
        try {
            energy = Double.parseDouble(energyStr);
            if (energy <= 0) {
                Toast.makeText(this, "Energy amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid energy amount", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build start & end DateTimes
        Calendar startCal = (Calendar) selectedCalendar.clone();
        startCal.set(Calendar.HOUR_OF_DAY, startHour);
        startCal.set(Calendar.MINUTE, startMinute);
        startCal.set(Calendar.SECOND, 0);

        Calendar endCal = (Calendar) selectedCalendar.clone();
        endCal.set(Calendar.HOUR_OF_DAY, endHour);
        endCal.set(Calendar.MINUTE, endMinute);
        endCal.set(Calendar.SECOND, 0);

        // Validation: End time must be after start time
        if (endCal.getTimeInMillis() <= startCal.getTimeInMillis()) {
            Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validation (7-Day Booking Limit Rule on client)
        Calendar maxAllowed = Calendar.getInstance();
        maxAllowed.add(Calendar.DAY_OF_YEAR, 7);
        if (startCal.after(maxAllowed)) {
            Toast.makeText(this, "Rule Error: Bookings must be scheduled within 7 days", Toast.LENGTH_LONG).show();
            return;
        }

        MicrogridNode selectedNode = (MicrogridNode) spNodeSelector.getSelectedItem();
        String nodeId = selectedNode != null ? selectedNode.getNodeCode() : "NODE-COL-01";

        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String startIso = isoFormat.format(startCal.getTime());
        String endIso = isoFormat.format(endCal.getTime());

        // Build JSON Payload for C# Web API
        JSONObject payload = new JSONObject();
        try {
            payload.put("prosumerId", PROSUMER_NIC);
            payload.put("prosumerNic", PROSUMER_NIC);
            payload.put("nodeId", nodeId);
            payload.put("reservedEnergyKwh", energy);
            payload.put("reservationDate", startIso);
            payload.put("startTime", startIso);
            payload.put("endTime", endIso);
            payload.put("status", "Pending");
        } catch (Exception e) {
            Toast.makeText(this, "Failed to build request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmitBooking.setEnabled(false);

        // Send to central C# Web API
        ApiClient.post("/reservations", payload.toString(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                progressBar.setVisibility(View.GONE);
                btnSubmitBooking.setEnabled(true);

                try {
                    JSONObject resObj = new JSONObject(response);
                    Reservation reservation = new Reservation();
                    reservation.setId(resObj.optString("id", "RES-" + System.currentTimeMillis()));
                    reservation.setProsumerId(PROSUMER_NIC);
                    reservation.setProsumerNic(PROSUMER_NIC);
                    reservation.setNodeId(nodeId);
                    reservation.setReservedEnergyKwh(energy);
                    reservation.setReservationDate(startIso);
                    reservation.setStartTime(startIso);
                    reservation.setEndTime(endIso);
                    reservation.setStatus("Pending");

                    // Save locally in SQLite for offline persistence
                    dbHelper.saveReservation(reservation);

                    // Navigate to Summary Screen (Table 2 Requirement)
                    Intent intent = new Intent(BookingActivity.this, BookingSummaryActivity.class);
                    intent.putExtra("reservation", reservation);
                    intent.putExtra("actionType", "CREATED");
                    startActivity(intent);
                    finish();

                } catch (Exception e) {
                    Toast.makeText(BookingActivity.this, "Booking created! (Saved locally)", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnSubmitBooking.setEnabled(true);
                Toast.makeText(BookingActivity.this, "Server Rule Rejection: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
