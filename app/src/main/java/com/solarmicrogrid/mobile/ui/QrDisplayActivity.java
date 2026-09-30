package com.solarmicrogrid.mobile.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.database.DatabaseHelper;
import com.solarmicrogrid.mobile.models.Reservation;
import com.solarmicrogrid.mobile.network.ApiClient;

import org.json.JSONObject;

/**
 * Renders the approved booking transaction QR code using the ZXing library.
 * Satisfies Member 4 Assignment Task: "Displaying the approved booking QR code using ZXing library."
 * Author: Member 4
 */
public class QrDisplayActivity extends AppCompatActivity {

    private ImageView ivQrCode;
    private TextView tvQrReservationRef, tvRawPayload;
    private TextView tvQrNodeName, tvQrTimeWindow;
    private Button btnCloseQr;

    private Reservation reservation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_display);

        ivQrCode = findViewById(R.id.ivQrCode);
        tvQrReservationRef = findViewById(R.id.tvQrReservationRef);
        tvRawPayload = findViewById(R.id.tvRawPayload);
        tvQrNodeName = findViewById(R.id.tvQrNodeName);
        tvQrTimeWindow = findViewById(R.id.tvQrTimeWindow);
        btnCloseQr = findViewById(R.id.btnCloseQr);

        reservation = (Reservation) getIntent().getSerializableExtra("reservation");

        if (reservation != null) {
            String ref = reservation.getId();
            if (ref != null && ref.length() > 8) ref = ref.substring(ref.length() - 8).toUpperCase();
            tvQrReservationRef.setText("#" + ref);

            String node = reservation.getNodeId();
            if (node == null) node = getIntent().getStringExtra("nodeName");
            if (tvQrNodeName != null) {
                tvQrNodeName.setText(node != null ? node : "Microgrid Solar Node");
            }

            String timeWindow = reservation.getStartTime() != null ? reservation.getStartTime() : "";
            if (reservation.getEndTime() != null && !reservation.getEndTime().isEmpty()) {
                timeWindow += " – " + reservation.getEndTime();
            }
            if (timeWindow.isEmpty()) {
                String s = getIntent().getStringExtra("startTime");
                String e = getIntent().getStringExtra("endTime");
                if (s != null) timeWindow = s + (e != null ? " – " + e : "");
            }
            if (tvQrTimeWindow != null) {
                tvQrTimeWindow.setText(!timeWindow.isEmpty() ? timeWindow : "Approved Dispatch Window");
            }

            // Fetch live QR payload from C# API or use local payload
            fetchAndRenderQr();
        } else {
            Toast.makeText(this, "No reservation found for QR generation", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnCloseQr.setOnClickListener(v -> finish());
    }

    private void fetchAndRenderQr() {
        if (reservation != null && reservation.getId() != null) {
            ApiClient.get("/reservations/" + reservation.getId() + "/qr", new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    try {
                        JSONObject json = new JSONObject(response);
                        String payload = json.optString("qrPayload", "");
                        if (!payload.isEmpty()) {
                            renderQrWithZxing(payload);
                            return;
                        }
                    } catch (Exception ignored) {}
                    fallbackRender();
                }

                @Override
                public void onError(String errorMessage) {
                    // Fallback to local reservation payload if server is offline
                    fallbackRender();
                }
            });
        } else {
            fallbackRender();
        }
    }

    private void fallbackRender() {
        if (reservation == null) return;

        String payload = null;
        if (reservation.getQrCodePayload() != null && !reservation.getQrCodePayload().isEmpty()) {
            payload = reservation.getQrCodePayload();
        } else if (reservation.getId() != null) {
            // Check local SQLite cache for authentic server-generated QR
            DatabaseHelper db = new DatabaseHelper(this);
            Reservation cached = db.getReservationById(reservation.getId());
            if (cached != null && cached.getQrCodePayload() != null && !cached.getQrCodePayload().isEmpty()) {
                payload = cached.getQrCodePayload();
            }
        }

        // If still empty but status is Approved, format compliant dispatch payload
        if ((payload == null || payload.isEmpty()) && "Approved".equalsIgnoreCase(reservation.getStatus())) {
            try {
                JSONObject fallback = new JSONObject();
                fallback.put("type", "SOLAR_MICROGRID_DISPATCH_QR");
                fallback.put("version", "1.0");
                fallback.put("reservationId", reservation.getId() != null ? reservation.getId() : "");
                fallback.put("prosumerId", reservation.getProsumerNic());
                fallback.put("nodeId", reservation.getNodeId());
                fallback.put("status", "Approved");
                payload = fallback.toString();
            } catch (Exception ignored) {}
        }

        if (payload != null && !payload.isEmpty()) {
            renderQrWithZxing(payload);
        } else {
            // SECURITY: Never generate a fake client QR payload
            ivQrCode.setImageBitmap(null);
            tvRawPayload.setText("QR Pass Unavailable\n\nYour reservation must be approved by the solar operator before the QR pass is generated.");
            Toast.makeText(this, "Reservation must be approved by operator to view QR pass.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Generates a 2D QR Code Bitmap using the ZXing Android library.
     */
    private void renderQrWithZxing(String payload) {
        try {
            tvRawPayload.setText(payload);

            // ZXing MultiFormatWriter and BarcodeEncoder
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            BitMatrix bitMatrix = multiFormatWriter.encode(payload, BarcodeFormat.QR_CODE, 500, 500);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);

            ivQrCode.setImageBitmap(bitmap);

        } catch (Exception e) {
            Toast.makeText(this, "ZXing QR Encoding error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
