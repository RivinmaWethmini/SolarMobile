package com.solarmicrogrid.mobile.ui;

import android.app.AlertDialog;
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
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.auth.SessionManager;
import com.solarmicrogrid.mobile.database.DatabaseHelper;
import com.solarmicrogrid.mobile.models.AuthUser;
import com.solarmicrogrid.mobile.models.MicrogridNode;
import com.solarmicrogrid.mobile.models.Reservation;
import com.solarmicrogrid.mobile.network.ApiClient;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 4-Step Interactive Wizard for Energy Slot Booking:
 * Step 1: Select Solar Station (Node)
 * Step 2: Choose Trading Date (+ 7-Day & 12-Hour Rule Help Dialog)
 * Step 3: Choose Trading Window (Presets + Custom Times)
 * Step 4: Energy Amount & Final Confirmation Review
 */
public class BookingActivity extends AppCompatActivity {

    // Step Progress Views
    private TextView tvStepProgressTitle, tvHeaderBackLabel;
    private View indicatorStep1, indicatorStep2, indicatorStep3, indicatorStep4;
    private TextView tvIndicator1, tvIndicator2, tvIndicator3, tvIndicator4;
    private int currentStep = 1;

    // Step 1 Views
    private View layoutStep1;
    private Spinner spNodeSelector;
    private TextView tvSelectedNodeInfo;
    private AppCompatButton btnStep1Next;

    // Step 2 Views
    private View layoutStep2;
    private Button btnSelectDate;
    private TextView tvDateRangeHint, tvRuleCheckBadgeDate, btnRuleHelp;
    private AppCompatButton btnStep2Prev, btnStep2Next;

    // Step 3 Views
    private View layoutStep3;
    private Button btnPresetMorning, btnPresetPeak, btnPresetAfternoon;
    private Button btnSelectStartTime, btnSelectEndTime;
    private TextView tvDurationBadge;
    private AppCompatButton btnStep3Prev, btnStep3Next;

    // Step 4 Views
    private View layoutStep4;
    private Button btnChip10, btnChip20, btnChip30, btnChip50;
    private EditText etEnergyAmount;
    private TextView tvPreviewNode, tvPreviewWindow, tvPreviewEnergy, tvRuleCheckBadge;
    private AppCompatButton btnStep4Prev;
    private Button btnSubmitBooking;
    private ProgressBar progressBar;

    // State Variables
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

        // Read authenticated user NIC with fallback
        AuthUser activeUser = SessionManager.getInstance(this).getUser();
        if (activeUser != null && activeUser.getNic() != null && !activeUser.getNic().trim().isEmpty()) {
            PROSUMER_NIC = activeUser.getNic();
        } else {
            android.content.SharedPreferences prefs = getSharedPreferences("solar_session", MODE_PRIVATE);
            String nic = prefs.getString("prosumer_nic", null);
            if (nic == null) nic = prefs.getString("nic", null);
            if (nic == null) nic = prefs.getString("username", null);
            PROSUMER_NIC = (nic != null && !nic.trim().isEmpty()) ? nic : "200224700740";
        }

        bindViews();
        setupStepNavigation();
        setupNodeSpinner();
        setupPickers();
        setupPresetChips();
        setupEnergyChips();
        setupRuleHelp();

        etEnergyAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int count, int after) { updateLivePreview(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSubmitBooking.setOnClickListener(v -> submitBookingRequest());

        goToStep(1);
        updateLivePreview();
    }

    private void bindViews() {
        // Header
        tvStepProgressTitle = findViewById(R.id.tvStepProgressTitle);
        tvHeaderBackLabel = findViewById(R.id.tvHeaderBackLabel);
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBack());
        }

        // Indicators
        indicatorStep1 = findViewById(R.id.indicatorStep1);
        indicatorStep2 = findViewById(R.id.indicatorStep2);
        indicatorStep3 = findViewById(R.id.indicatorStep3);
        indicatorStep4 = findViewById(R.id.indicatorStep4);

        tvIndicator1 = findViewById(R.id.tvIndicator1);
        tvIndicator2 = findViewById(R.id.tvIndicator2);
        tvIndicator3 = findViewById(R.id.tvIndicator3);
        tvIndicator4 = findViewById(R.id.tvIndicator4);

        // Step 1
        layoutStep1 = findViewById(R.id.layoutStep1);
        spNodeSelector = findViewById(R.id.spNodeSelector);
        tvSelectedNodeInfo = findViewById(R.id.tvSelectedNodeInfo);
        btnStep1Next = findViewById(R.id.btnStep1Next);

        // Step 2
        layoutStep2 = findViewById(R.id.layoutStep2);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        tvDateRangeHint = findViewById(R.id.tvDateRangeHint);
        tvRuleCheckBadgeDate = findViewById(R.id.tvRuleCheckBadgeDate);
        btnRuleHelp = findViewById(R.id.btnRuleHelp);
        btnStep2Prev = findViewById(R.id.btnStep2Prev);
        btnStep2Next = findViewById(R.id.btnStep2Next);

        // Step 3
        layoutStep3 = findViewById(R.id.layoutStep3);
        btnPresetMorning = findViewById(R.id.btnPresetMorning);
        btnPresetPeak = findViewById(R.id.btnPresetPeak);
        btnPresetAfternoon = findViewById(R.id.btnPresetAfternoon);
        btnSelectStartTime = findViewById(R.id.btnSelectStartTime);
        btnSelectEndTime = findViewById(R.id.btnSelectEndTime);
        tvDurationBadge = findViewById(R.id.tvDurationBadge);
        btnStep3Prev = findViewById(R.id.btnStep3Prev);
        btnStep3Next = findViewById(R.id.btnStep3Next);

        // Step 4
        layoutStep4 = findViewById(R.id.layoutStep4);
        btnChip10 = findViewById(R.id.btnChip10);
        btnChip20 = findViewById(R.id.btnChip20);
        btnChip30 = findViewById(R.id.btnChip30);
        btnChip50 = findViewById(R.id.btnChip50);
        etEnergyAmount = findViewById(R.id.etEnergyAmount);

        tvPreviewNode = findViewById(R.id.tvPreviewNode);
        tvPreviewWindow = findViewById(R.id.tvPreviewWindow);
        tvPreviewEnergy = findViewById(R.id.tvPreviewEnergy);
        tvRuleCheckBadge = findViewById(R.id.tvRuleCheckBadge);

        btnStep4Prev = findViewById(R.id.btnStep4Prev);
        btnSubmitBooking = findViewById(R.id.btnSubmitBooking);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupStepNavigation() {
        btnStep1Next.setOnClickListener(v -> goToStep(2));

        btnStep2Prev.setOnClickListener(v -> goToStep(1));
        btnStep2Next.setOnClickListener(v -> {
            // Validation before proceeding to Step 3
            Calendar maxLimit = Calendar.getInstance();
            maxLimit.add(Calendar.DAY_OF_YEAR, 7);
            if (selectedCalendar.after(maxLimit)) {
                Toast.makeText(this, "Please choose a date within 7 days", Toast.LENGTH_SHORT).show();
                return;
            }
            goToStep(3);
        });

        btnStep3Prev.setOnClickListener(v -> goToStep(2));
        btnStep3Next.setOnClickListener(v -> {
            int durationMinutes = (endHour * 60 + endMinute) - (startHour * 60 + startMinute);
            if (durationMinutes <= 0) {
                Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show();
                return;
            }
            goToStep(4);
        });

        btnStep4Prev.setOnClickListener(v -> goToStep(3));
    }

    private void goToStep(int step) {
        currentStep = step;

        layoutStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        layoutStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        layoutStep3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        layoutStep4.setVisibility(step == 4 ? View.VISIBLE : View.GONE);

        // Smooth fade-in on active layout
        View activeLayout = (step == 1) ? layoutStep1 : (step == 2) ? layoutStep2 : (step == 3) ? layoutStep3 : layoutStep4;
        activeLayout.setAlpha(0f);
        activeLayout.animate().alpha(1f).setDuration(200).start();

        // Update Header Titles
        switch (step) {
            case 1:
                tvStepProgressTitle.setText("Step 1 of 4: Select Solar Station");
                tvHeaderBackLabel.setText("Dashboard");
                break;
            case 2:
                tvStepProgressTitle.setText("Step 2 of 4: Choose Trading Date");
                tvHeaderBackLabel.setText("Station");
                break;
            case 3:
                tvStepProgressTitle.setText("Step 3 of 4: Choose Trading Window");
                tvHeaderBackLabel.setText("Date");
                break;
            case 4:
                tvStepProgressTitle.setText("Step 4 of 4: Energy Amount & Review");
                tvHeaderBackLabel.setText("Time");
                break;
        }

        updateBreadcrumbStyles(step);
        updateLivePreview();
    }

    private void updateBreadcrumbStyles(int step) {
        View[] indicators = {indicatorStep1, indicatorStep2, indicatorStep3, indicatorStep4};
        TextView[] textViews = {tvIndicator1, tvIndicator2, tvIndicator3, tvIndicator4};
        String[] labels = {"1 Station", "2 Date", "3 Time", "4 Confirm"};

        for (int i = 0; i < 4; i++) {
            int stepNum = i + 1;
            if (stepNum == step) {
                indicators[i].setBackgroundResource(R.drawable.bg_tab_selected);
                textViews[i].setTextColor(ContextCompat.getColor(this, R.color.bg_black));
                textViews[i].setText(labels[i]);
            } else if (stepNum < step) {
                indicators[i].setBackgroundResource(R.drawable.bg_pill_dark);
                textViews[i].setTextColor(ContextCompat.getColor(this, R.color.emerald_approved));
                textViews[i].setText("✓ " + labels[i].substring(2));
            } else {
                indicators[i].setBackgroundResource(R.drawable.bg_dark_input);
                textViews[i].setTextColor(ContextCompat.getColor(this, R.color.text_light_secondary));
                textViews[i].setText(labels[i]);
            }
        }
    }

    private void handleBack() {
        if (currentStep > 1) {
            goToStep(currentStep - 1);
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentStep > 1) {
            goToStep(currentStep - 1);
        } else {
            super.onBackPressed();
        }
    }

    private void setupRuleHelp() {
        btnRuleHelp.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Microgrid Schedule Rules")
                    .setMessage("📅 7-Day Advance Schedule:\n"
                            + "You can book energy slots up to 7 days in advance. This ensures accurate solar generation forecasts.\n\n"
                            + "⏱️ 12-Hour Advance Notice:\n"
                            + "Reservations must be submitted at least 12 hours before slot start time so microgrid operators can prepare dispatch safely.\n\n"
                            + "⚠️ Cancellation Restriction:\n"
                            + "Reservations scheduled with less than 12 hours notice cannot be modified or cancelled later.")
                    .setPositiveButton("Understood", null)
                    .show();
        });
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
                                    obj.optString("nodeCode", "Station-" + (i + 1)),
                                    obj.optString("name", "Solar Station " + (i + 1)),
                                    obj.optString("region", "Colombo Region"),
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
            nodesList.add(new MicrogridNode("node-01", "Station-01", "Colombo North Solar Hub", "Western", 500));
            nodesList.add(new MicrogridNode("node-02", "Station-02", "Kaduwela Microgrid Station", "Western", 350));
            nodesList.add(new MicrogridNode("node-03", "Station-03", "Kandy Central Solar Station", "Central", 400));
            nodesList.add(new MicrogridNode("node-04", "Station-04", "Galle Coastal Solar Array", "Southern", 600));
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
                        tvSelectedNodeInfo.setText(n.getName() + " • " + n.getRegion() + " (" + String.format(Locale.getDefault(), "%.0f kW Capacity)", n.getTotalCapacityKw()));
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

            datePicker.getDatePicker().setMinDate(now.getTimeInMillis());
            Calendar maxLimit = Calendar.getInstance();
            maxLimit.add(Calendar.DAY_OF_YEAR, 7);
            datePicker.getDatePicker().setMaxDate(maxLimit.getTimeInMillis());
            datePicker.show();
        });

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
        if (spNodeSelector != null && spNodeSelector.getSelectedItem() instanceof MicrogridNode) {
            MicrogridNode node = (MicrogridNode) spNodeSelector.getSelectedItem();
            String code = node.getNodeCode();
            if (code != null && !code.trim().isEmpty()) {
                tvPreviewNode.setText(node.getName() + " (" + code + ")");
            } else {
                tvPreviewNode.setText(node.getName());
            }
        } else {
            tvPreviewNode.setText("Solar Microgrid Station");
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
            tvDurationBadge.setText("Invalid: End time must be after start time");
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
        String ruleNotice;
        int ruleColor;
        if (diffHours < 12) {
            ruleNotice = "⚠️ Notice: Less than 12h advance notice (Cannot be modified after booking)";
            ruleColor = Color.parseColor("#FFD000");
        } else {
            ruleNotice = "✓ Complies with 7-Day Limit and 12-Hour notice rule";
            ruleColor = Color.parseColor("#10B981");
        }

        tvRuleCheckBadge.setText(ruleNotice);
        tvRuleCheckBadge.setTextColor(ruleColor);
        if (tvRuleCheckBadgeDate != null) {
            tvRuleCheckBadgeDate.setText(ruleNotice);
            tvRuleCheckBadgeDate.setTextColor(ruleColor);
        }
    }

    private void submitBookingRequest() {
        String energyStr = etEnergyAmount.getText().toString().trim();
        if (energyStr.isEmpty()) {
            Toast.makeText(this, "Please enter reserved energy amount (kWh)", Toast.LENGTH_SHORT).show();
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

        Calendar startCal = (Calendar) selectedCalendar.clone();
        startCal.set(Calendar.HOUR_OF_DAY, startHour);
        startCal.set(Calendar.MINUTE, startMinute);
        startCal.set(Calendar.SECOND, 0);

        Calendar endCal = (Calendar) selectedCalendar.clone();
        endCal.set(Calendar.HOUR_OF_DAY, endHour);
        endCal.set(Calendar.MINUTE, endMinute);
        endCal.set(Calendar.SECOND, 0);

        if (endCal.getTimeInMillis() <= startCal.getTimeInMillis()) {
            Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar maxAllowed = Calendar.getInstance();
        maxAllowed.add(Calendar.DAY_OF_YEAR, 7);
        if (startCal.after(maxAllowed)) {
            Toast.makeText(this, "Rule Error: Bookings must be scheduled within 7 days", Toast.LENGTH_LONG).show();
            return;
        }

        MicrogridNode selectedNode = (MicrogridNode) spNodeSelector.getSelectedItem();
        String nodeId = selectedNode != null ? selectedNode.getNodeCode() : "Station-01";

        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String startIso = isoFormat.format(startCal.getTime());
        String endIso = isoFormat.format(endCal.getTime());

        JSONObject payload = new JSONObject();
        try {
            payload.put("prosumerId", PROSUMER_NIC);
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

                    dbHelper.saveReservation(reservation);

                    Intent intent = new Intent(BookingActivity.this, BookingSummaryActivity.class);
                    intent.putExtra("reservation", reservation);
                    intent.putExtra("actionType", "CREATED");
                    startActivity(intent);
                    finish();

                } catch (Exception e) {
                    Toast.makeText(BookingActivity.this, "Booking created!", Toast.LENGTH_SHORT).show();
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
