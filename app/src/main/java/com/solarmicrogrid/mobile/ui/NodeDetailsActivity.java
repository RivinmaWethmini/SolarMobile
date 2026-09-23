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
                ContextCompat.getColor(this, R.color.slate_card)
        );

        readIntentData();
        bindViews();
    }

    private void readIntentData() {
        Intent intent = getIntent();

        nodeId = intent.getStringExtra("nodeId");
        nodeName = intent.getStringExtra("nodeName");
        latitude = intent.getDoubleExtra("latitude", 0);
        longitude = intent.getDoubleExtra("longitude", 0);
        capacityKWh = intent.getDoubleExtra("capacityKWh", 0);
        batterySlots = intent.getIntExtra("batterySlots", 0);
        schedule = intent.getStringExtra("schedule");
        status = intent.getStringExtra("status");

        if (nodeId == null || nodeId.trim().isEmpty()) {
            nodeId = "Unknown";
        }

        if (nodeName == null || nodeName.trim().isEmpty()) {
            nodeName = "Unnamed Node";
        }

        if (schedule == null || schedule.trim().isEmpty()) {
            schedule = "Schedule not specified";
        }

        if (status == null || status.trim().isEmpty()) {
            status = "Inactive";
        }
    }

    private void bindViews() {
        TextView btnBack = findViewById(R.id.btnBack);
        TextView tvDetailsNodeName = findViewById(R.id.tvDetailsNodeName);
        TextView tvDetailsNodeId = findViewById(R.id.tvDetailsNodeId);
        TextView tvDetailsStatus = findViewById(R.id.tvDetailsStatus);
        TextView tvDetailsLocation = findViewById(R.id.tvDetailsLocation);
        TextView tvDetailsCapacity = findViewById(R.id.tvDetailsCapacity);
        TextView tvDetailsBatterySlots = findViewById(
                R.id.tvDetailsBatterySlots
        );
        TextView tvDetailsSchedule = findViewById(
                R.id.tvDetailsSchedule
        );
        AppCompatButton btnBookNode = findViewById(
                R.id.btnBookNode
        );

        tvDetailsNodeName.setText(nodeName);
        tvDetailsNodeId.setText(nodeId);
        tvDetailsStatus.setText(status.toUpperCase(Locale.US));

        tvDetailsLocation.setText(
                String.format(
                        Locale.US,
                        "%.4f, %.4f",
                        latitude,
                        longitude
                )
        );

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
            tvDetailsStatus.setBackgroundColor(
                    ContextCompat.getColor(
                            this,
                            R.color.emerald_approved
                    )
            );

            btnBookNode.setEnabled(true);
            btnBookNode.setText("Book Energy Slot");
        } else {
            tvDetailsStatus.setBackgroundColor(
                    ContextCompat.getColor(
                            this,
                            R.color.slate_muted
                    )
            );

            btnBookNode.setEnabled(false);
            btnBookNode.setText("Node Currently Inactive");
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