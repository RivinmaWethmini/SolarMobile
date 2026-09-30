package com.solarmicrogrid.mobile.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.database.DatabaseHelper;
import com.solarmicrogrid.mobile.models.Reservation;
import com.solarmicrogrid.mobile.network.ApiClient;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Screen for modifying an existing energy slot reservation according to business rules:
 * - 12-hour advance notice requirement
 * - 7-day advance booking limit
 * - Server-controlled state and QR dispatch
 */
public class ModifyBookingActivity extends AppCompatActivity {

    private TextView tvModifyRef, tvModifyNic, tvModifyNode, tvModifyStatus;
    private EditText etModifyEnergy;
    private Button btnSelectDate, btnSelectStartTime, btnSelectEndTime, btnSubmitModify;
    private ProgressBar pbModify;

    private Reservation reservation;
    private Calendar selectedCalendar;
    private int startHour = 9, startMinute = 0;
    private int endHour = 11, endMinute = 0;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_booking);

        dbHelper = new DatabaseHelper(this);
        selectedCalendar = Calendar.getInstance();

        reservation = (Reservation) getIntent().getSerializableExtra("reservation");
        if (reservation == null) {
            Toast.makeText(this, "Reservation data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvModifyRef = findViewById(R.id.tvModifyRef);
        tvModifyNic = findViewById(R.id.tvModifyNic);
        tvModifyNode = findViewById(R.id.tvModifyNode);
        tvModifyStatus = findViewById(R.id.tvModifyStatus);
        etModifyEnergy = findViewById(R.id.etModifyEnergy);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectStartTime = findViewById(R.id.btnSelectStartTime);
        btnSelectEndTime = findViewById(R.id.btnSelectEndTime);
        btnSubmitModify = findViewById(R.id.btnSubmitModify);
        pbModify = findViewById(R.id.pbModify);

        Button btnCancel = findViewById(R.id.btnCancelModification);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }

        setupInitialValues();
        setupPickers();

        btnSubmitModify.setOnClickListener(v -> submitModification());
    }

    private void setupInitialValues() {
        String ref = reservation.getId();
        if (ref != null && ref.length() > 8) ref = ref.substring(ref.length() - 8).toUpperCase();
        tvModifyRef.setText("#" + ref);
        tvModifyNic.setText(reservation.getProsumerNic());
        tvModifyNode.setText(reservation.getNodeId());
        String status = reservation.getStatus() != null ? reservation.getStatus() : "Pending";
        tvModifyStatus.setText(status);
        if ("Approved".equalsIgnoreCase(status)) {
            tvModifyStatus.setTextColor(getResources().getColor(R.color.emerald_approved));
        } else if ("Cancelled".equalsIgnoreCase(status)) {
            tvModifyStatus.setTextColor(getResources().getColor(R.color.slate_cancelled));
        } else if ("Rejected".equalsIgnoreCase(status)) {
            tvModifyStatus.setTextColor(getResources().getColor(R.color.red_rejected));
        } else {
            tvModifyStatus.setTextColor(getResources().getColor(R.color.yellow_primary));
        }
        etModifyEnergy.setText(String.valueOf(reservation.getReservedEnergyKwh()));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        btnSelectDate.setText(sdf.format(selectedCalendar.getTime()));
        btnSelectStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute));
        btnSelectEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute));
    }

    private void setupPickers() {
        btnSelectDate.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog dp = new DatePickerDialog(this, (view, year, month, day) -> {
                selectedCalendar.set(Calendar.YEAR, year);
                selectedCalendar.set(Calendar.MONTH, month);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, day);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                btnSelectDate.setText(sdf.format(selectedCalendar.getTime()));
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

            dp.getDatePicker().setMinDate(now.getTimeInMillis());
            Calendar maxDate = Calendar.getInstance();
            maxDate.add(Calendar.DAY_OF_YEAR, 7);
            dp.getDatePicker().setMaxDate(maxDate.getTimeInMillis());
            dp.show();
        });

        btnSelectStartTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                startHour = hourOfDay;
                startMinute = minute;
                btnSelectStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute));
            }, startHour, startMinute, true).show();
        });

        btnSelectEndTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                endHour = hourOfDay;
                endMinute = minute;
                btnSelectEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute));
            }, endHour, endMinute, true).show();
        });
    }

    private void submitModification() {
        String energyStr = etModifyEnergy.getText().toString().trim();
        double energy;
        try {
            energy = Double.parseDouble(energyStr);
            if (energy <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            Toast.makeText(this, "Energy amount must be positive", Toast.LENGTH_SHORT).show();
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

        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String startIso = isoFormat.format(startCal.getTime());
        String endIso = isoFormat.format(endCal.getTime());

        JSONObject json = new JSONObject();
        try {
            json.put("id", reservation.getId());
            json.put("prosumerId", reservation.getProsumerId());
            json.put("nodeId", reservation.getNodeId());
            json.put("reservedEnergyKwh", energy);
            json.put("reservationDate", startIso);
            json.put("startTime", startIso);
            json.put("endTime", endIso);
            json.put("status", reservation.getStatus());
        } catch (Exception ex) {
            Toast.makeText(this, "JSON Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        pbModify.setVisibility(View.VISIBLE);
        btnSubmitModify.setEnabled(false);

        ApiClient.put("/reservations/" + reservation.getId(), json.toString(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                pbModify.setVisibility(View.GONE);
                btnSubmitModify.setEnabled(true);

                reservation.setReservedEnergyKwh(energy);
                reservation.setStartTime(startIso);
                reservation.setEndTime(endIso);
                reservation.setReservationDate(startIso);

                dbHelper.saveReservation(reservation);

                Toast.makeText(ModifyBookingActivity.this, "Reservation modified successfully", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(ModifyBookingActivity.this, BookingSummaryActivity.class);
                intent.putExtra("reservation", reservation);
                intent.putExtra("actionType", "MODIFIED");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                pbModify.setVisibility(View.GONE);
                btnSubmitModify.setEnabled(true);
                new AlertDialog.Builder(ModifyBookingActivity.this)
                        .setTitle("Modification Rejected")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }
}
