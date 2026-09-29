package com.solarmicrogrid.mobile.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.network.ApiClient;

import org.json.JSONObject;

import java.util.List;

/**
 * Native Android QR Code Scanner for Grid Station Operators.
 * Captures prosumer QR pass via device camera, sends scanned payload
 * to SolarAPI POST /api/qr/verify, and authorizes or rejects energy dispatch.
 * 
 * Author: Member 4 (Energy Reservation & QR Dispatch)
 */
public class OperatorScanActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 101;

    private DecoratedBarcodeView barcodeView;
    private LinearLayout layoutScanResult;
    private TextView tvScanHint;
    private TextView tvScanResultStatus;
    private TextView tvScanResultMessage;
    private TextView tvScanResultDetails;
    private Button btnScanAgain;
    private Button btnCancelScan;

    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_scan);

        barcodeView = findViewById(R.id.barcodeScanner);
        layoutScanResult = findViewById(R.id.layoutScanResult);
        tvScanHint = findViewById(R.id.tvScanHint);
        tvScanResultStatus = findViewById(R.id.tvScanResultStatus);
        tvScanResultMessage = findViewById(R.id.tvScanResultMessage);
        tvScanResultDetails = findViewById(R.id.tvScanResultDetails);
        btnScanAgain = findViewById(R.id.btnScanAgain);
        btnCancelScan = findViewById(R.id.btnCancelScan);

        btnCancelScan.setOnClickListener(v -> finish());

        btnScanAgain.setOnClickListener(v -> {
            isProcessing = false;
            layoutScanResult.setVisibility(View.GONE);
            tvScanHint.setVisibility(View.VISIBLE);
            barcodeView.resume();
        });

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST
            );
        } else {
            initBarcodeScanner();
        }
    }

    private void initBarcodeScanner() {
        barcodeView.initializeFromIntent(getIntent());
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (isProcessing || result.getText() == null) {
                    return;
                }
                isProcessing = true;
                barcodeView.pause();
                sendToVerificationApi(result.getText());
            }

            @Override
            public void possibleResultPoints(List<com.google.zxing.ResultPoint> resultPoints) {
                // Optional preview point handling
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initBarcodeScanner();
                barcodeView.resume();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR passes.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /**
     * Submits the raw scanned QR payload to SolarAPI for cryptographic validation,
     * status checking, and dispatch recording.
     */
    private void sendToVerificationApi(String scannedPayload) {
        tvScanHint.setVisibility(View.GONE);
        layoutScanResult.setVisibility(View.VISIBLE);
        tvScanResultStatus.setText("VERIFYING PASS...");
        tvScanResultStatus.setTextColor(ContextCompat.getColor(this, R.color.yellow_primary));
        tvScanResultMessage.setText("Validating security signature with microgrid server...");
        tvScanResultDetails.setText("");

        try {
            JSONObject body = new JSONObject();
            body.put("scannedPayload", scannedPayload);
            body.put("operatorId", "MOBILE-OPERATOR-01");

            ApiClient.post("/qr/verify", body.toString(), new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.optBoolean("success", false);
                        String message = json.optString("message", "");
                        String prosumerId = json.optString("prosumerId", "");
                        String nodeId = json.optString("nodeId", "");
                        double energy = json.optDouble("reservedEnergyKwh", 0);
                        String reservationDate = json.optString("reservationDate", "");

                        if (success) {
                            tvScanResultStatus.setText("✓ DISPATCH AUTHORIZED");
                            tvScanResultStatus.setTextColor(ContextCompat.getColor(OperatorScanActivity.this, R.color.emerald_approved));
                            tvScanResultMessage.setText(message);
                            tvScanResultDetails.setText(
                                    "Prosumer NIC: " + prosumerId + "\n" +
                                    "Node ID: " + nodeId + "\n" +
                                    "Energy: " + energy + " kWh\n" +
                                    "Schedule: " + reservationDate
                            );
                        } else {
                            tvScanResultStatus.setText("✗ VERIFICATION REJECTED");
                            tvScanResultStatus.setTextColor(ContextCompat.getColor(OperatorScanActivity.this, R.color.red_rejected));
                            tvScanResultMessage.setText(message);
                            tvScanResultDetails.setText("Dispatch transfer has been denied by security checks.");
                        }
                    } catch (Exception e) {
                        showError("Failed to parse verification response.");
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    showError(errorMessage);
                }
            });
        } catch (Exception e) {
            showError("Failed to format verification request.");
        }
    }

    private void showError(String message) {
        tvScanResultStatus.setText("✗ VERIFICATION FAILED");
        tvScanResultStatus.setTextColor(ContextCompat.getColor(this, R.color.red_rejected));
        tvScanResultMessage.setText(message);
        tvScanResultDetails.setText("Check network connection or try scanning again.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}
