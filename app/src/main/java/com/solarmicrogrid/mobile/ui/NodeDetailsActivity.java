package com.solarmicrogrid.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.solarmicrogrid.mobile.R;

import java.util.Locale;

public class NodeDetailsActivity extends AppCompatActivity {

    private String nodeId;
    private String nodeName;
    private String nodeCode;
    private String region;
    private double latitude;
    private double longitude;
    private double capacityKWh;
    private int batterySlots;
    private String schedule;
    private String status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_node_details);

        getWindow().setStatusBarColor(
                ContextCompat.getColor(this, R.color.bg_black)
        );

        readIntentData();
        bindViews();
    }

    private void readIntentData() {
        Intent intent = getIntent();

        nodeId = intent.getStringExtra("nodeId");
        nodeCode = intent.getStringExtra("nodeCode");
        nodeName = intent.getStringExtra("nodeName");
        region = intent.getStringExtra("region");
        latitude = intent.getDoubleExtra("latitude", 0);
        longitude = intent.getDoubleExtra("longitude", 0);
        capacityKWh = intent.getDoubleExtra("capacityKWh", 0);
        batterySlots = intent.getIntExtra("batterySlots", 0);
        schedule = intent.getStringExtra("schedule");
        status = intent.getStringExtra("status");

        if (nodeId == null || nodeId.trim().isEmpty()) {
            nodeId = "Station-01";
        }

        if (nodeName == null || nodeName.trim().isEmpty()) {
            nodeName = "Solar Microgrid Station";
        }

        if (region == null || region.trim().isEmpty()) {
            region = "Colombo Western Region";
        }

        if (schedule == null || schedule.trim().isEmpty() || schedule.contains("not specified")) {
            schedule = "06:00 - 18:00 Daily";
        }

        if (status == null || status.trim().isEmpty()) {
            status = "Active";
        }
    }

    private void bindViews() {
        TextView btnBack = findViewById(R.id.btnBack);
        TextView tvDetailsNodeName = findViewById(R.id.tvDetailsNodeName);
        TextView tvDetailsNodeId = findViewById(R.id.tvDetailsNodeId);
        TextView tvDetailsStatus = findViewById(R.id.tvDetailsStatus);
        TextView tvDetailsLocation = findViewById(R.id.tvDetailsLocation);
        TextView tvDetailsCapacity = findViewById(R.id.tvDetailsCapacity);
        TextView tvDetailsBatterySlots = findViewById(R.id.tvDetailsBatterySlots);
        TextView tvDetailsSchedule = findViewById(R.id.tvDetailsSchedule);
        AppCompatButton btnBookNode = findViewById(R.id.btnBookNode);

        tvDetailsNodeName.setText(nodeName);

        if (nodeCode != null && !nodeCode.trim().isEmpty()) {
            tvDetailsNodeId.setText("Station: " + nodeCode);
        } else if (nodeId.length() > 8) {
            tvDetailsNodeId.setText("Station #" + nodeId.substring(nodeId.length() - 8).toUpperCase(Locale.US));
        } else {
            tvDetailsNodeId.setText("Station #" + nodeId);
        }

        // Clean Title Case for status (Strictly NO ALL-CAPS)
        String cleanStatus = status != null && !status.isEmpty()
                ? status.substring(0, 1).toUpperCase(Locale.US) + status.substring(1).toLowerCase(Locale.US)
                : "Active";
        tvDetailsStatus.setText(cleanStatus);

        if (latitude != 0 || longitude != 0) {
            tvDetailsLocation.setText(
                    String.format(
                            Locale.US,
                            "%.4f, %.4f (%s)",
                            latitude,
                            longitude,
                            region
                    )
            );
        } else {
            tvDetailsLocation.setText(region);
        }

        if (capacityKWh >= 1000) {
            tvDetailsCapacity.setText(
                    String.format(
                            Locale.US,
                            "%.2f MWh",
                            capacityKWh / 1000
                    )
            );
        } else {
            tvDetailsCapacity.setText(
                    String.format(
                            Locale.US,
                            "%.0f kWh",
                            capacityKWh
                    )
            );
        }

        tvDetailsBatterySlots.setText(
                String.format(
                        Locale.US,
                        "%d slots",
                        batterySlots
                )
        );

        tvDetailsSchedule.setText(schedule);

        boolean isActive = "Active".equalsIgnoreCase(status);

        if (isActive) {
            if (tvDetailsStatus.getBackground() != null) {
                tvDetailsStatus.getBackground().mutate().setTint(
                        ContextCompat.getColor(this, R.color.emerald_approved)
                );
            }

            btnBookNode.setEnabled(true);
            btnBookNode.setText("Book Energy Slot");
        } else {
            if (tvDetailsStatus.getBackground() != null) {
                tvDetailsStatus.getBackground().mutate().setTint(
                        ContextCompat.getColor(this, R.color.slate_muted)
                );
            }

            btnBookNode.setEnabled(false);
            btnBookNode.setText("Station Currently Inactive");
        }

        btnBack.setOnClickListener(view -> finish());

        btnBookNode.setOnClickListener(view -> {
            Intent intent = new Intent(
                    NodeDetailsActivity.this,
                    BookingActivity.class
            );

            intent.putExtra("nodeId", nodeId);
            intent.putExtra("nodeName", nodeName);
            intent.putExtra("capacityKWh", capacityKWh);
            intent.putExtra("batterySlots", batterySlots);
            intent.putExtra("schedule", schedule);

            startActivity(intent);
        });
    }
}
