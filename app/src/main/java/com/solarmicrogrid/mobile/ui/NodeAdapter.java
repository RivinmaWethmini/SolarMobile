package com.solarmicrogrid.mobile.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.models.MicrogridNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NodeAdapter extends RecyclerView.Adapter<NodeAdapter.NodeViewHolder> {

    private final Context context;
    private List<MicrogridNode> nodeList;

    public NodeAdapter(Context context, List<MicrogridNode> list) {
        this.context = context;
        this.nodeList = list != null ? list : new ArrayList<>();
    }

    public void updateList(List<MicrogridNode> newList) {
        nodeList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NodeViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_node, parent, false);

        return new NodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull NodeViewHolder holder,
            int position
    ) {
        MicrogridNode node = nodeList.get(position);

        String name = node.getName();

        if (name == null || name.trim().isEmpty()) {
            name = "Unnamed Node";
        }

        holder.tvNodeName.setText(name);

        String nodeId = node.getId();
        String displayedId = "UNKNOWN";

        if (nodeId != null && !nodeId.trim().isEmpty()) {
            displayedId = nodeId;

            if (displayedId.length() > 8) {
                displayedId = displayedId.substring(
                        displayedId.length() - 8
                );
            }

            displayedId = displayedId.toUpperCase(Locale.US);
        }

        holder.tvNodeId.setText("#" + displayedId);

        holder.tvNodeLocation.setText(
                String.format(
                        Locale.US,
                        "📍 %.4f, %.4f",
                        node.getLatitude(),
                        node.getLongitude()
                )
        );

        double capacity = node.getTotalCapacityKw();

        if (capacity >= 1000) {
            holder.tvNodeCapacity.setText(
                    String.format(
                            Locale.US,
                            "⚡ %.2f MWh",
                            capacity / 1000
                    )
            );
        } else {
            holder.tvNodeCapacity.setText(
                    String.format(
                            Locale.US,
                            "⚡ %.0f kWh",
                            capacity
                    )
            );
        }

        holder.tvNodeBattery.setText(
                String.format(
                        Locale.US,
                        "🔋 %d slots",
                        node.getBatterySlots()
                )
        );

        String schedule = node.getSchedule();

        if (schedule == null || schedule.trim().isEmpty()) {
            holder.tvNodeSchedule.setText("🕒 Schedule not specified");
        } else {
            holder.tvNodeSchedule.setText("🕒 " + schedule);
        }

        String status = node.getStatus();

        if (status == null || status.trim().isEmpty()) {
            status = "Inactive";
        }

        boolean isActive = "Active".equalsIgnoreCase(status);

        holder.tvNodeStatus.setText(status.toUpperCase(Locale.US));

        int accentColor;
        int statusColor;

        if (isActive) {
            accentColor = ContextCompat.getColor(
                    context,
                    R.color.emerald_approved
            );

            statusColor = ContextCompat.getColor(
                    context,
                    R.color.emerald_approved
            );
        } else {
            accentColor = ContextCompat.getColor(
                    context,
                    R.color.red_rejected
            );

            statusColor = ContextCompat.getColor(
                    context,
                    R.color.slate_muted
            );
        }

        holder.viewStatusBar.setBackgroundColor(accentColor);
        if (holder.tvNodeStatus.getBackground() != null) {
            holder.tvNodeStatus.getBackground().mutate().setTint(statusColor);
        }

        holder.btnViewDetails.setOnClickListener(
                view -> openNodeDetails(node)
        );

        holder.itemView.setOnClickListener(
                view -> openNodeDetails(node)
        );
    }

    private void openNodeDetails(MicrogridNode node) {
        Intent intent = new Intent(context, NodeDetailsActivity.class);

        intent.putExtra("nodeId", node.getId());
        intent.putExtra("nodeName", node.getName());
        intent.putExtra("latitude", node.getLatitude());
        intent.putExtra("longitude", node.getLongitude());
        intent.putExtra("capacityKWh", node.getTotalCapacityKw());
        intent.putExtra("batterySlots", node.getBatterySlots());
        intent.putExtra("schedule", node.getSchedule());
        intent.putExtra("status", node.getStatus());

        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return nodeList.size();
    }

    static class NodeViewHolder extends RecyclerView.ViewHolder {

        TextView tvNodeName;
        TextView tvNodeId;
        TextView tvNodeLocation;
        TextView tvNodeCapacity;
        TextView tvNodeBattery;
        TextView tvNodeSchedule;
        TextView tvNodeStatus;
        AppCompatButton btnViewDetails;
        View viewStatusBar;

        NodeViewHolder(@NonNull View itemView) {
            super(itemView);

            tvNodeName = itemView.findViewById(R.id.tvNodeName);
            tvNodeId = itemView.findViewById(R.id.tvNodeId);
            tvNodeLocation = itemView.findViewById(R.id.tvNodeLocation);
            tvNodeCapacity = itemView.findViewById(R.id.tvNodeCapacity);
            tvNodeBattery = itemView.findViewById(R.id.tvNodeBattery);
            tvNodeSchedule = itemView.findViewById(R.id.tvNodeSchedule);
            tvNodeStatus = itemView.findViewById(R.id.tvNodeStatus);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            viewStatusBar = itemView.findViewById(
                    R.id.viewNodeStatusBar
            );
        }
    }
}
