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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Native Android QR Code Scanner for Grid Station Operators.
 * Captures prosumer QR pass via device camera or 1-tap Fast Verify action,
 * sends scanned payload to SolarAPI POST /api/qr/verify, and authorizes dispatch.
 */
public class OperatorScanActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 101;

    private DecoratedBarcodeView barcodeView;
    private View layoutCameraPermission;
    private Button btnGrantPermission;
    private LinearLayout layoutScanResult;
    private View layoutBottomGuide;
    private TextView tvScanHint;
    private TextView tvScanResultStatus;
    private TextView tvScanResultMessage;
    private TextView tvScanResultDetails;
    private Button btnScanAgain;
    private Button btnCancelScan;
    private Button btnFastTestPass;

    private boolean isProcessing = false;
    private boolean isScannerInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_scan);

        barcodeView = findViewById(R.id.barcodeScanner);
        layoutCameraPermission = findViewById(R.id.layoutCameraPermission);
        btnGrantPermission = findViewById(R.id.btnGrantPermission);
        layoutScanResult = findViewById(R.id.layoutScanResult);
        layoutBottomGuide = findViewById(R.id.layoutBottomGuide);
        tvScanHint = findViewById(R.id.tvScanHint);
        tvScanResultStatus = findViewById(R.id.tvScanResultStatus);
        tvScanResultMessage = findViewById(R.id.tvScanResultMessage);
        tvScanResultDetails = findViewById(R.id.tvScanResultDetails);
        btnScanAgain = findViewById(R.id.btnScanAgain);
        btnCancelScan = findViewById(R.id.btnCancelScan);
        btnFastTestPass = findViewById(R.id.btnFastTestPass);

        btnCancelScan.setOnClickListener(v -> finish());

        btnScanAgain.setOnClickListener(v -> {
            isProcessing = false;
            layoutScanResult.setVisibility(View.GONE);
            if (layoutBottomGuide != null) layoutBottomGuide.setVisibility(View.VISIBLE);
            if (isScannerInitialized) {
                barcodeView.resume();
            }
        });

        if (btnGrantPermission != null) {
            btnGrantPermission.setOnClickListener(v -> {
                ActivityCompat.requestPermissions(
                        OperatorScanActivity.this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST
                );
            });
        }

        // Fast Test Mode: Allows instant test verification of the latest approved reservation
        if (btnFastTestPass != null) {
            btnFastTestPass.setOnClickListener(v -> runFastTestVerification());
        }

        checkAndInitCamera();
    }

    private void checkAndInitCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (layoutCameraPermission != null) {
                layoutCameraPermission.setVisibility(View.VISIBLE);
            }
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST
            );
        } else {
            if (layoutCameraPermission != null) {
                layoutCameraPermission.setVisibility(View.GONE);
            }
            initBarcodeScanner();
        }
    }

    private void initBarcodeScanner() {
        if (isScannerInitialized) return;
        try {
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
                }
            });
            isScannerInitialized = true;
            barcodeView.resume();
        } catch (Exception e) {
            Toast.makeText(this, "Camera init warning: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (layoutCameraPermission != null) {
                    layoutCameraPermission.setVisibility(View.GONE);
                }
                initBarcodeScanner();
                barcodeView.resume();
            } else {
                if (layoutCameraPermission != null) {
                    layoutCameraPermission.setVisibility(View.VISIBLE);
                }
                Toast.makeText(this, "Camera permission needed to scan physical QR passes. You can also use Fast-Verify below!", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Queries approved reservations from SolarAPI and verifies the first approved slot's QR pass.
     */
    private void runFastTestVerification() {
        Toast.makeText(this, "Fetching approved pass from server...", Toast.LENGTH_SHORT).show();
        if (isScannerInitialized) {
            barcodeView.pause();
        }

        ApiClient.get("/reservations", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONArray arr = new JSONArray(response);
                    JSONObject targetReservation = null;

                    // Find first Approved reservation
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject r = arr.getJSONObject(i);
                        String status = r.optString("status", "");
                        if ("Approved".equalsIgnoreCase(status) && !r.optBoolean("isDispatched", false)) {
                            targetReservation = r;
                            break;
                        }
                    }

                    if (targetReservation == null && arr.length() > 0) {
                        targetReservation = arr.getJSONObject(0);
                    }

                    if (targetReservation != null) {
                        String resId = targetReservation.optString("id", "");
                        String prosumerId = targetReservation.optString("prosumerId", "200224700740");
                        String nodeId = targetReservation.optString("nodeId", "NODE-COL-01");
                        String resDate = targetReservation.optString("reservationDate", "");
                        String hash = targetReservation.optString("verificationHash", "DEMO-SEC-TOKEN");

                        JSONObject qrPayload = new JSONObject();
                        qrPayload.put("type", "SOLAR_MICROGRID_DISPATCH_QR");
                        qrPayload.put("version", "1.0");
                        qrPayload.put("reservationId", resId);
                        qrPayload.put("prosumerId", prosumerId);
                        qrPayload.put("nodeId", nodeId);
                        qrPayload.put("reservationDate", resDate);
                        qrPayload.put("status", "Approved");
                        qrPayload.put("securityToken", hash);

                        sendToVerificationApi(qrPayload.toString());
                    } else {
                        showError("No approved reservations found in system to test. Please book a slot first!");
                    }
                } catch (Exception e) {
                    showError("Error preparing test QR: " + e.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                showError("Could not fetch test reservations: " + errorMessage);
            }
        });
    }

    private void sendToVerificationApi(String scannedPayload) {
        if (layoutBottomGuide != null) layoutBottomGuide.setVisibility(View.GONE);
        layoutScanResult.setVisibility(View.VISIBLE);
        tvScanResultStatus.setText("Verifying energy pass...");
        tvScanResultStatus.setTextColor(ContextCompat.getColor(this, R.color.yellow_primary));
        tvScanResultMessage.setText("Checking reservation pass details with solar server...");
        tvScanResultDetails.setText("");

        try {
            JSONObject body = new JSONObject();
            body.put("scannedPayload", scannedPayload);
            body.put("operatorId", "MOBILE-OPERATOR-COL01");

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
                            tvScanResultStatus.setText("✓ Pass Verified & Authorized");
                            tvScanResultStatus.setTextColor(ContextCompat.getColor(OperatorScanActivity.this, R.color.emerald_approved));
                            tvScanResultMessage.setText(message);
                            tvScanResultDetails.setText(
                                    "Prosumer NIC: " + prosumerId + "\n" +
                                    "Station: " + nodeId + "\n" +
                                    "Energy Amount: " + energy + " kWh\n" +
                                    "Slot Date: " + reservationDate
                            );
                        } else {
                            tvScanResultStatus.setText("✗ Verification Failed");
                            tvScanResultStatus.setTextColor(ContextCompat.getColor(OperatorScanActivity.this, R.color.red_rejected));
                            tvScanResultMessage.setText(message);
                            tvScanResultDetails.setText("Energy pass could not be verified.");
                        }
                    } catch (Exception e) {
                        showError("Failed to read server response: " + e.getMessage());
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
        tvScanResultStatus.setText("Verification Result");
        tvScanResultStatus.setTextColor(ContextCompat.getColor(this, R.color.red_rejected));
        tvScanResultMessage.setText(message);
        tvScanResultDetails.setText("Check network connection or try scanning again.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            if (layoutCameraPermission != null) {
                layoutCameraPermission.setVisibility(View.GONE);
            }
            if (isScannerInitialized && !isProcessing) {
                barcodeView.resume();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isScannerInitialized) {
            barcodeView.pause();
        }
    }
}
