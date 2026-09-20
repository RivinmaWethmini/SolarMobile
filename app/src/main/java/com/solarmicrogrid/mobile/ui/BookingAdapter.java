package com.solarmicrogrid.mobile.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.models.Reservation;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying energy reservations.
 */
public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private final Context context;
    private List<Reservation> reservationsList;

    public BookingAdapter(Context context, List<Reservation> list) {
        this.context = context;
        this.reservationsList = list != null ? list : new ArrayList<>();
    }

    public void updateList(List<Reservation> newList) {
        this.reservationsList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Reservation res = reservationsList.get(position);

        String id = res.getId();
        if (id != null && id.length() > 8) id = id.substring(id.length() - 8).toUpperCase();
        holder.tvCardId.setText("#" + id);

        String status = res.getStatus() != null ? res.getStatus() : "Pending";
        holder.tvCardStatus.setText(status.toUpperCase());

        int accentColor;
        int badgeBgColor;
        if ("Approved".equalsIgnoreCase(status)) {
            accentColor = context.getResources().getColor(R.color.emerald_approved);
            badgeBgColor = 0xFF10B981;
            holder.tvCardStatus.setTextColor(0xFF0F172A);
        } else if ("Cancelled".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status)) {
            accentColor = context.getResources().getColor(R.color.red_rejected);
            badgeBgColor = 0xFFEF4444;
            holder.tvCardStatus.setTextColor(0xFFFFFFFF);
        } else {
            // Pending
            accentColor = context.getResources().getColor(R.color.amber_pending);
            badgeBgColor = 0xFFF59E0B;
            holder.tvCardStatus.setTextColor(0xFF0F172A);
        }

        // Set left accent status bar color
        if (holder.viewStatusBar != null) {
            holder.viewStatusBar.setBackgroundColor(accentColor);
        }
        // Set status badge pill background
        holder.tvCardStatus.setBackgroundColor(badgeBgColor);

        holder.tvCardNode.setText(res.getNodeId() != null ? res.getNodeId() : "Unknown Node");
        holder.tvCardEnergy.setText("⚡ " + res.getReservedEnergyKwh() + " kW/h");

        // Format time: show just HH:MM if full ISO string
        String startTime = res.getStartTime() != null ? res.getStartTime() : "";
        if (startTime.contains("T")) {
            String[] parts = startTime.split("T");
            startTime = parts.length > 1 ? parts[1].substring(0, Math.min(5, parts[1].length())) : startTime;
        }
        holder.tvCardTime.setText(startTime);

        holder.btnCardAction.setOnClickListener(v -> {
            Intent intent = new Intent(context, BookingSummaryActivity.class);
            intent.putExtra("reservation", res);
            context.startActivity(intent);
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, BookingSummaryActivity.class);
            intent.putExtra("reservation", res);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return reservationsList.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvCardId, tvCardStatus, tvCardNode, tvCardEnergy, tvCardTime;
        Button btnCardAction;
        View viewStatusBar;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCardId = itemView.findViewById(R.id.tvCardId);
            tvCardStatus = itemView.findViewById(R.id.tvCardStatus);
            tvCardNode = itemView.findViewById(R.id.tvCardNode);
            tvCardEnergy = itemView.findViewById(R.id.tvCardEnergy);
            tvCardTime = itemView.findViewById(R.id.tvCardTime);
            btnCardAction = itemView.findViewById(R.id.btnCardAction);
            viewStatusBar = itemView.findViewById(R.id.viewStatusBar);
        }
    }
}
