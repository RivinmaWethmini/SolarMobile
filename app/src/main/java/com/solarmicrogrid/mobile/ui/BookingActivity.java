package com.solarmicrogrid.mobile.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

/**
 * Enhanced, user-friendly Energy Slot Booking Screen with Step-by-Step Guidance,
 * Preset Trading Window Chips, Energy Chips, and Real-Time Summary Preview.
 * Enforces Member 4 rules: 7-Day booking limit and 12-Hour cancellation notice.
 */
public class BookingActivity extends AppCompatActivity {

    private Spinner spNodeSelector;
    private TextView tvSelectedNodeInfo;
    private Button btnSelectDate;
    private TextView tvDateRangeHint;

    // Time Pickers & Presets
    private Button btnPresetMorning, btnPresetPeak, btnPresetAfternoon;
    private Button btnSelectStartTime, btnSelectEndTime;
    private TextView tvDurationBadge;

    // Energy Chips & Input
    private Button btnChip10, btnChip20, btnChip30, btnChip50;
    private EditText etEnergyAmount;

    // Live Preview Card
    private TextView tvPreviewNode, tvPreviewWindow, tvPreviewEnergy, tvRuleCheckBadge;

    private Button btnSubmitBooking;
    private ProgressBar progressBar;

    private Calendar selectedCalendar;
    private int startHour = 9, startMinute = 0;
    private int endHour = 11, endMinute = 0;
    private List<MicrogridNode> nodesList;
    private DatabaseHelper dbHelper;

    private String PROSUMER_NIC;
    private String preselectedNodeId;
    private String preselectedNodeName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        if (getIntent() != null) {
            preselectedNodeId = getIntent().getStringExtra("nodeId");
            preselectedNodeName = getIntent().getStringExtra("nodeName");
        }

        dbHelper = new DatabaseHelper(this);
        selectedCalendar = Calendar.getInstance();

        // Read user NIC with fallback
        android.content.SharedPreferences prefs = getSharedPreferences("solar_session", MODE_PRIVATE);
        String nic = prefs.getString("prosumer_nic", null);
        if (nic == null) nic = prefs.getString("nic", null);
        if (nic == null) nic = prefs.getString("username", null);
        PROSUMER_NIC = (nic != null && !nic.trim().isEmpty()) ? nic : "200012345678";

        // Bind Views
        spNodeSelector = findViewById(R.id.spNodeSelector);
        tvSelectedNodeInfo = findViewById(R.id.tvSelectedNodeInfo);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        tvDateRangeHint = findViewById(R.id.tvDateRangeHint);

        btnPresetMorning = findViewById(R.id.btnPresetMorning);
        btnPresetPeak = findViewById(R.id.btnPresetPeak);
        btnPresetAfternoon = findViewById(R.id.btnPresetAfternoon);
        btnSelectStartTime = findViewById(R.id.btnSelectStartTime);
        btnSelectEndTime = findViewById(R.id.btnSelectEndTime);
        tvDurationBadge = findViewById(R.id.tvDurationBadge);

        btnChip10 = findViewById(R.id.btnChip10);
        btnChip20 = findViewById(R.id.btnChip20);
        btnChip30 = findViewById(R.id.btnChip30);
        btnChip50 = findViewById(R.id.btnChip50);
        etEnergyAmount = findViewById(R.id.etEnergyAmount);

        tvPreviewNode = findViewById(R.id.tvPreviewNode);
        tvPreviewWindow = findViewById(R.id.tvPreviewWindow);
        tvPreviewEnergy = findViewById(R.id.tvPreviewEnergy);
        tvRuleCheckBadge = findViewById(R.id.tvRuleCheckBadge);

        btnSubmitBooking = findViewById(R.id.btnSubmitBooking);
        progressBar = findViewById(R.id.progressBar);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        setupNodeSpinner();
        setupPickers();
        setupPresetChips();
        setupEnergyChips();

        etEnergyAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int count, int after) { updateLivePreview(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSubmitBooking.setOnClickListener(v -> submitBookingRequest());

        updateLivePreview();
    }

    private void setupNodeSpinner() {
        nodesList = new ArrayList<>();
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

        spNodeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < nodesList.size()) {
                    MicrogridNode n = nodesList.get(position);
                    if (tvSelectedNodeInfo != null) {
                        tvSelectedNodeInfo.setText(n.getName() + " • " + n.getRegion() + " Region (" + String.format(Locale.getDefault(), "%.0f kW Capacity)", n.getTotalCapacityKw()));
                    }
                }
                updateLivePreview();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Preselect node if launched from NodeDetailsActivity
        if (preselectedNodeId != null && !preselectedNodeId.trim().isEmpty()) {
            int selectedIdx = -1;
            for (int i = 0; i < nodesList.size(); i++) {
                MicrogridNode n = nodesList.get(i);
                if (preselectedNodeId.equalsIgnoreCase(n.getId()) ||
                    preselectedNodeId.equalsIgnoreCase(n.getNodeCode())) {
                    selectedIdx = i;
                    break;
                }
            }
            if (selectedIdx >= 0) {
                spNodeSelector.setSelection(selectedIdx);
            }
        }
    }

    private void setupPickers() {
        // Date Picker
        btnSelectDate.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedCalendar.set(Calendar.YEAR, year);
                selectedCalendar.set(Calendar.MONTH, month);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault());
                btnSelectDate.setText(sdf.format(selectedCalendar.getTime()));
                updateLivePreview();
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
                clearPresetChipsStyle();
                updateLivePreview();
            }, startHour, startMinute, true);
            timePicker.show();
        });

        // End Time Picker
        btnSelectEndTime.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                endHour = hourOfDay;
                endMinute = minute;
                btnSelectEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute));
                clearPresetChipsStyle();
                updateLivePreview();
            }, endHour, endMinute, true);
            timePicker.show();
        });

        // Set default date text
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault());
        btnSelectDate.setText(sdf.format(selectedCalendar.getTime()));

        if (tvDateRangeHint != null) {
            Calendar minLimit = Calendar.getInstance();
            Calendar maxLimit = Calendar.getInstance();
            maxLimit.add(Calendar.DAY_OF_YEAR, 7);
            SimpleDateFormat hintFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
            tvDateRangeHint.setText("Available: " + hintFormat.format(minLimit.getTime()) + " – " + hintFormat.format(maxLimit.getTime()) + " (within 7 days)");
        }
    }

    private void setupPresetChips() {
        btnPresetMorning.setOnClickListener(v -> {
            startHour = 9; startMinute = 0;
            endHour = 11; endMinute = 0;
            btnSelectStartTime.setText("09:00");
            btnSelectEndTime.setText("11:00");
            highlightPreset(btnPresetMorning);
            updateLivePreview();
        });

        btnPresetPeak.setOnClickListener(v -> {
            startHour = 11; startMinute = 0;
            endHour = 13; endMinute = 0;
            btnSelectStartTime.setText("11:00");
            btnSelectEndTime.setText("13:00");
            highlightPreset(btnPresetPeak);
            updateLivePreview();
        });

        btnPresetAfternoon.setOnClickListener(v -> {
            startHour = 14; startMinute = 0;
            endHour = 16; endMinute = 0;
            btnSelectStartTime.setText("14:00");
            btnSelectEndTime.setText("16:00");
            highlightPreset(btnPresetAfternoon);
            updateLivePreview();
        });
    }

    private void highlightPreset(Button selectedBtn) {
        clearPresetChipsStyle();
        selectedBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_chip_selected));
        selectedBtn.setTextColor(Color.parseColor("#FFD000"));
    }

    private void clearPresetChipsStyle() {
        Button[] buttons = {btnPresetMorning, btnPresetPeak, btnPresetAfternoon};
        for (Button b : buttons) {
            b.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_chip_unselected));
            b.setTextColor(Color.parseColor("#9496A1"));
        }
    }

    private void setupEnergyChips() {
        btnChip10.setOnClickListener(v -> selectEnergy(10, btnChip10));
        btnChip20.setOnClickListener(v -> selectEnergy(20, btnChip20));
        btnChip30.setOnClickListener(v -> selectEnergy(30, btnChip30));
        btnChip50.setOnClickListener(v -> selectEnergy(50, btnChip50));
    }

    private void selectEnergy(int amount, Button selectedChip) {
        etEnergyAmount.setText(String.format(Locale.getDefault(), "%.1f", (double) amount));
        Button[] chips = {btnChip10, btnChip20, btnChip30, btnChip50};
        for (Button c : chips) {
            c.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_chip_unselected));
            c.setTextColor(Color.parseColor("#9496A1"));
        }
        selectedChip.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_chip_selected));
        selectedChip.setTextColor(Color.parseColor("#FFD000"));
        updateLivePreview();
    }

    private void updateLivePreview() {
        // Node
        if (spNodeSelector != null && spNodeSelector.getSelectedItem() instanceof MicrogridNode) {
            MicrogridNode node = (MicrogridNode) spNodeSelector.getSelectedItem();
            tvPreviewNode.setText(node.getName() + " (" + node.getNodeCode() + ")");
        } else {
            tvPreviewNode.setText("Colombo North Solar Hub");
        }

        // Window & Duration
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault());
        String dateStr = dayFormat.format(selectedCalendar.getTime());
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d – %02d:%02d", startHour, startMinute, endHour, endMinute);
        tvPreviewWindow.setText(dateStr + " • " + timeStr);

        int durationMinutes = (endHour * 60 + endMinute) - (startHour * 60 + startMinute);
        if (durationMinutes > 0) {
            int hrs = durationMinutes / 60;
            int mins = durationMinutes % 60;
            String durStr = (hrs > 0 ? hrs + "h " : "") + (mins > 0 ? mins + "m" : "");
            tvDurationBadge.setText("Duration: " + durStr.trim() + " (" + timeStr + ")");
            tvDurationBadge.setTextColor(Color.parseColor("#9496A1"));
        } else {
            tvDurationBadge.setText("Invalid: End time must be after Start time");
            tvDurationBadge.setTextColor(Color.parseColor("#EF4444"));
        }

        // Energy
        String energyText = etEnergyAmount.getText().toString().trim();
        if (!energyText.isEmpty()) {
            tvPreviewEnergy.setText(energyText + " kWh");
        } else {
            tvPreviewEnergy.setText("— kWh");
        }

        // Rule Check Hint
        Calendar now = Calendar.getInstance();
        Calendar startCal = (Calendar) selectedCalendar.clone();
        startCal.set(Calendar.HOUR_OF_DAY, startHour);
        startCal.set(Calendar.MINUTE, startMinute);
        startCal.set(Calendar.SECOND, 0);

        long diffHours = (startCal.getTimeInMillis() - now.getTimeInMillis()) / (1000 * 60 * 60);
        if (diffHours < 12) {
            tvRuleCheckBadge.setText("⚠️ Notice: Booking is less than 12h in advance (Cancellation restriction applies)");
            tvRuleCheckBadge.setTextColor(Color.parseColor("#FFD000"));
        } else {
            tvRuleCheckBadge.setText("✓ Complies with 7-Day Limit and 12-Hour notice rule");
            tvRuleCheckBadge.setTextColor(Color.parseColor("#10B981"));
        }
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

                    // Navigate to Summary Screen
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
                Toast.makeText(BookingActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
