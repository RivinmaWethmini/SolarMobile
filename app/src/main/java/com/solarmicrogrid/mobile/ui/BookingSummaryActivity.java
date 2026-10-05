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
    private Button btnViewQrCode, btnModifyBooking, btnCancelBooking, btnBackHome;

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
        btnModifyBooking = findViewById(R.id.btnModifyBooking);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);
        btnBackHome = findViewById(R.id.btnBackHome);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        reservation = (Reservation) getIntent().getSerializableExtra("reservation");
        if (getIntent().hasExtra("actionType")) {
            actionType = getIntent().getStringExtra("actionType");
        }

        populateDetails();

        btnBackHome.setOnClickListener(v -> {
            Intent homeIntent = new Intent(BookingSummaryActivity.this, MainActivity.class);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(homeIntent);
            finish();
        });

        // Modify Reservation (Subject to 12-Hour Notice Rule)
        btnModifyBooking.setOnClickListener(v -> {
            Intent modifyIntent = new Intent(BookingSummaryActivity.this, ModifyBookingActivity.class);
            modifyIntent.putExtra("reservation", reservation);
            startActivity(modifyIntent);
        });

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
        } else if ("MODIFIED".equals(actionType)) {
            tvActionStatus.setText("Reservation Modified Successfully!");
            tvActionStatus.setTextColor(getResources().getColor(R.color.accent_solar));
        } else if ("CANCELLED".equals(actionType)) {
            tvActionStatus.setText("Reservation Cancelled (12-Hour Notice Verified)");
            tvActionStatus.setTextColor(getResources().getColor(R.color.red_rejected));
        } else {
            tvActionStatus.setText("Reservation Details");
            tvActionStatus.setTextColor(getResources().getColor(R.color.slate_text_dim));
        }

        String refId = reservation.getId();
        if (refId != null && refId.length() > 8) {
            refId = refId.substring(refId.length() - 8).toUpperCase();
        }
        tvSummaryId.setText("#" + refId);
        tvSummaryNic.setText(reservation.getProsumerNic() != null ? reservation.getProsumerNic() : "—");
        tvSummaryNode.setText(reservation.getNodeId() != null ? reservation.getNodeId() : "—");
        tvSummaryCapacity.setText(reservation.getReservedEnergyKwh() + " kWh");

        String timeWindow = reservation.getStartTime() != null ? reservation.getStartTime() : "";
        if (reservation.getEndTime() != null && !reservation.getEndTime().isEmpty()) {
            timeWindow += " – " + reservation.getEndTime();
        }
        tvSummaryTime.setText(timeWindow.isEmpty() ? "—" : timeWindow);

        String status = reservation.getStatus() != null ? reservation.getStatus() : "Pending";
        tvSummaryStatus.setText(status);

        boolean isPending = "Pending".equalsIgnoreCase(status);
        boolean isApproved = "Approved".equalsIgnoreCase(status);
        boolean isCancelled = "Cancelled".equalsIgnoreCase(status);
        boolean isRejected = "Rejected".equalsIgnoreCase(status);

        android.graphics.drawable.GradientDrawable statusBg = new android.graphics.drawable.GradientDrawable();
        statusBg.setCornerRadius(8f * getResources().getDisplayMetrics().density);

        if (isPending) {
            statusBg.setColor(0xFFFFD000);
            tvSummaryStatus.setBackground(statusBg);
            tvSummaryStatus.setTextColor(0xFF0A0A0C);
            btnModifyBooking.setVisibility(View.VISIBLE);
            btnCancelBooking.setVisibility(View.VISIBLE);
            btnViewQrCode.setVisibility(View.GONE);
        } else if (isApproved) {
            statusBg.setColor(0xFF10B981);
            tvSummaryStatus.setBackground(statusBg);
            tvSummaryStatus.setTextColor(0xFFFFFFFF);
            btnModifyBooking.setVisibility(View.VISIBLE);
            btnCancelBooking.setVisibility(View.VISIBLE);
            btnViewQrCode.setVisibility(View.VISIBLE);
        } else if (isCancelled) {
            statusBg.setColor(0xFF374151);
            tvSummaryStatus.setBackground(statusBg);
            tvSummaryStatus.setTextColor(0xFF9CA3AF);
            btnModifyBooking.setVisibility(View.GONE);
            btnCancelBooking.setVisibility(View.GONE);
            btnViewQrCode.setVisibility(View.GONE);
        } else if (isRejected) {
            statusBg.setColor(0xFF7F1D1D);
            tvSummaryStatus.setBackground(statusBg);
            tvSummaryStatus.setTextColor(0xFFFCA5A5);
            btnModifyBooking.setVisibility(View.GONE);
            btnCancelBooking.setVisibility(View.GONE);
            btnViewQrCode.setVisibility(View.GONE);
        }
    }

    private void confirmAndCancelReservation() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Reservation")
                .setMessage("Are you sure you want to cancel this booking?\n\nNote: Rule requires at least 12 hours notice prior to scheduled start time.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> executeCancelOnServer())
                .setNegativeButton("Keep Booking", null)
                .show();
    }

    private void executeCancelOnServer() {
        if (reservation.getId() == null) return;

        btnCancelBooking.setEnabled(false);
        ApiClient.post("/reservations/" + reservation.getId() + "/cancel", "{}", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                btnCancelBooking.setEnabled(true);
                Toast.makeText(BookingSummaryActivity.this, "Reservation cancelled successfully", Toast.LENGTH_SHORT).show();

                // Update local status and clear QR credentials
                reservation.setStatus("Cancelled");
                reservation.setQrCodePayload("");
                dbHelper.updateReservationStatus(reservation.getId(), "Cancelled");

                actionType = "CANCELLED";
                populateDetails();
            }

            @Override
            public void onError(String errorMessage) {
                btnCancelBooking.setEnabled(true);
                new AlertDialog.Builder(BookingSummaryActivity.this)
                        .setTitle("Cancellation Blocked")
                        .setMessage("Server Rule Violation:\n\n" + errorMessage)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }
}
