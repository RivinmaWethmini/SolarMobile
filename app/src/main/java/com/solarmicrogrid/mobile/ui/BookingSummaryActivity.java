package com.solarmicrogrid.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.database.DatabaseHelper;
import com.solarmicrogrid.mobile.models.Reservation;
import com.solarmicrogrid.mobile.network.ApiClient;

/**
 * Summary page displayed after each action (Creation, Modification, or Cancellation).
 * Enforces the 12-hour advance cancellation rule and allows QR code display when approved.
 * Author: Member 4
 */
public class BookingSummaryActivity extends AppCompatActivity {

    private TextView tvSummaryTitle, tvActionStatus, tvSummaryId, tvSummaryNic;
    private TextView tvSummaryNode, tvSummaryCapacity, tvSummaryTime, tvSummaryStatus;
    private Button btnViewQrCode, btnCancelBooking, btnBackHome;

    private Reservation reservation;
    private String actionType = "VIEW";
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_summary);

        dbHelper = new DatabaseHelper(this);

        tvSummaryTitle = findViewById(R.id.tvSummaryTitle);
        tvActionStatus = findViewById(R.id.tvActionStatus);
        tvSummaryId = findViewById(R.id.tvSummaryId);
        tvSummaryNic = findViewById(R.id.tvSummaryNic);
        tvSummaryNode = findViewById(R.id.tvSummaryNode);
        tvSummaryCapacity = findViewById(R.id.tvSummaryCapacity);
        tvSummaryTime = findViewById(R.id.tvSummaryTime);
        tvSummaryStatus = findViewById(R.id.tvSummaryStatus);

        btnViewQrCode = findViewById(R.id.btnViewQrCode);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);
        btnBackHome = findViewById(R.id.btnBackHome);

        reservation = (Reservation) getIntent().getSerializableExtra("reservation");
        if (getIntent().hasExtra("actionType")) {
            actionType = getIntent().getStringExtra("actionType");
        }

        populateDetails();

        btnBackHome.setOnClickListener(v -> finish());

        // Cancel Reservation (Subject to 12-Hour Rule)
        btnCancelBooking.setOnClickListener(v -> confirmAndCancelReservation());

        // View QR Code Button (ZXing integration for approved bookings)
        btnViewQrCode.setOnClickListener(v -> {
            Intent qrIntent = new Intent(BookingSummaryActivity.this, QrDisplayActivity.class);
            qrIntent.putExtra("reservation", reservation);
            startActivity(qrIntent);
        });
    }

    private void populateDetails() {
        if (reservation == null) {
            Toast.makeText(this, "No reservation data found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if ("CREATED".equals(actionType)) {
            tvActionStatus.setText("Energy Slot Reservation Created!");
            tvActionStatus.setTextColor(getResources().getColor(R.color.emerald_approved));
        } else if ("CANCELLED".equals(actionType)) {
            tvActionStatus.setText("Reservation Cancelled (12-Hour Notice Verified)");
            tvActionStatus.setTextColor(getResources().getColor(R.color.red_rejected));
            btnCancelBooking.setVisibility(View.GONE);
        }

        String refId = reservation.getId();
        if (refId != null && refId.length() > 8) {
            refId = refId.substring(refId.length() - 8).toUpperCase();
        }
        tvSummaryId.setText("#" + refId);
        tvSummaryNic.setText(reservation.getProsumerNic());
        tvSummaryNode.setText(reservation.getNodeId());
        tvSummaryCapacity.setText(reservation.getReservedEnergyKwh() + " kW/h");
        tvSummaryTime.setText(reservation.getStartTime() + " to " + reservation.getEndTime());
        tvSummaryStatus.setText(reservation.getStatus());

        boolean isApproved = "Approved".equalsIgnoreCase(reservation.getStatus());
        boolean isCancelled = "Cancelled".equalsIgnoreCase(reservation.getStatus());

        if (isApproved) {
            tvSummaryStatus.setTextColor(getResources().getColor(R.color.emerald_approved));
            btnViewQrCode.setVisibility(View.VISIBLE); // Reveal ZXing QR Code button
        } else if (isCancelled) {
            tvSummaryStatus.setTextColor(getResources().getColor(R.color.red_rejected));
            btnCancelBooking.setVisibility(View.GONE);
            btnViewQrCode.setVisibility(View.GONE);
        } else {
            tvSummaryStatus.setTextColor(getResources().getColor(R.color.amber_pending));
            btnViewQrCode.setVisibility(View.GONE);
        }
    }

    private void confirmAndCancelReservation() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Reservation")
                .setMessage("Are you sure you want to cancel this booking?\n\nNote: FAT Service rule requires at least 12 hours notice prior to scheduled start time.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> executeCancelOnServer())
                .setNegativeButton("Keep Booking", null)
                .show();
    }

    private void executeCancelOnServer() {
        if (reservation.getId() == null) return;

        btnCancelBooking.setEnabled(false);
        ApiClient.put("/Reservation/" + reservation.getId() + "/cancel", "{}", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                btnCancelBooking.setEnabled(true);
                Toast.makeText(BookingSummaryActivity.this, "Reservation cancelled successfully", Toast.LENGTH_SHORT).show();

                // Update local status
                reservation.setStatus("Cancelled");
                dbHelper.updateReservationStatus(reservation.getId(), "Cancelled");

                actionType = "CANCELLED";
                populateDetails();
            }

            @Override
            public void onError(String errorMessage) {
                btnCancelBooking.setEnabled(true);
                // Displays 12-hour rule rejection message from central C# API
                new AlertDialog.Builder(BookingSummaryActivity.this)
                        .setTitle("Cancellation Blocked")
                        .setMessage("Server Rule Violation:\n\n" + errorMessage)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }
}
