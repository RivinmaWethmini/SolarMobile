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
        holder.tvCardStatus.setText(status);

        int accentColor;
        int badgeBgColor;
        int badgeTextColor;

        if ("Approved".equalsIgnoreCase(status)) {
            accentColor = 0xFF10B981;
            badgeBgColor = 0xFF10B981;
            badgeTextColor = 0xFFFFFFFF;
            holder.btnCardAction.setText("View QR Pass →");
            holder.btnCardAction.setBackground(context.getDrawable(R.drawable.bg_yellow_pill));
            holder.btnCardAction.setTextColor(0xFF0A0A0C);
            holder.btnCardAction.setOnClickListener(v -> {
                Intent intent = new Intent(context, QrDisplayActivity.class);
                intent.putExtra("reservation", res);
                context.startActivity(intent);
            });
        } else if ("Cancelled".equalsIgnoreCase(status)) {
            accentColor = 0xFF6B7280;
            badgeBgColor = 0xFF374151;
            badgeTextColor = 0xFF9CA3AF;
            holder.btnCardAction.setText("View Details →");
            holder.btnCardAction.setBackground(context.getDrawable(R.drawable.bg_pill_dark));
            holder.btnCardAction.setTextColor(0xFF9CA3AF);
            holder.btnCardAction.setOnClickListener(v -> {
                Intent intent = new Intent(context, BookingSummaryActivity.class);
                intent.putExtra("reservation", res);
                context.startActivity(intent);
            });
        } else if ("Rejected".equalsIgnoreCase(status)) {
            accentColor = 0xFFEF4444;
            badgeBgColor = 0xFF7F1D1D;
            badgeTextColor = 0xFFFCA5A5;
            holder.btnCardAction.setText("View Details →");
            holder.btnCardAction.setBackground(context.getDrawable(R.drawable.bg_pill_dark));
            holder.btnCardAction.setTextColor(0xFFEF4444);
            holder.btnCardAction.setOnClickListener(v -> {
                Intent intent = new Intent(context, BookingSummaryActivity.class);
                intent.putExtra("reservation", res);
                context.startActivity(intent);
            });
        } else {
            // Pending
            accentColor = 0xFFFFD000;
            badgeBgColor = 0xFFFFD000;
            badgeTextColor = 0xFF0A0A0C;
            holder.btnCardAction.setText("View Details →");
            holder.btnCardAction.setBackground(context.getDrawable(R.drawable.bg_pill_dark));
            holder.btnCardAction.setTextColor(0xFFFFD000);
            holder.btnCardAction.setOnClickListener(v -> {
                Intent intent = new Intent(context, BookingSummaryActivity.class);
                intent.putExtra("reservation", res);
                context.startActivity(intent);
            });
        }

        // Set left accent status bar color
        if (holder.viewStatusBar != null) {
            holder.viewStatusBar.setBackgroundColor(accentColor);
        }
        // Set status badge pill background and text color
        if (holder.tvCardStatus.getBackground() != null) {
            holder.tvCardStatus.getBackground().mutate().setTint(badgeBgColor);
        }
        holder.tvCardStatus.setTextColor(badgeTextColor);

        holder.tvCardNode.setText(res.getNodeId() != null ? res.getNodeId() : "Microgrid Node");
        holder.tvCardEnergy.setText(res.getReservedEnergyKwh() + " kWh");

        // Format Date (e.g. Mon, 25 Sep 2026)
        String rawStart = res.getStartTime() != null ? res.getStartTime() : "";
        String formattedDate = formatDateString(rawStart);
        if (holder.tvCardDate != null) {
            if (!formattedDate.isEmpty()) {
                holder.tvCardDate.setVisibility(View.VISIBLE);
                holder.tvCardDate.setText(formattedDate);
            } else {
                holder.tvCardDate.setVisibility(View.GONE);
            }
        }

        // Format Time Window (e.g. 09:00 – 11:00)
        String startTimeFormatted = extractTime(res.getStartTime());
        String endTimeFormatted = extractTime(res.getEndTime());
        String timeDisplay = startTimeFormatted;
        if (!endTimeFormatted.isEmpty()) {
            timeDisplay = startTimeFormatted + " – " + endTimeFormatted;
        }
        holder.tvCardTime.setText(timeDisplay);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, BookingSummaryActivity.class);
            intent.putExtra("reservation", res);
            context.startActivity(intent);
        });
    }

    private String formatDateString(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return "";
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault());
                java.util.Date d = sdf.parse(dateStr);
                if (d != null) {
                    java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("EEE, dd MMM yyyy", java.util.Locale.getDefault());
                    return out.format(d);
                }
            } catch (Exception ignored) {}
        }
        return dateStr.contains("T") ? dateStr.split("T")[0] : dateStr;
    }

    private String extractTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return "";
        if (timeStr.contains("T")) {
            String[] parts = timeStr.split("T");
            if (parts.length > 1) {
                return parts[1].length() >= 5 ? parts[1].substring(0, 5) : parts[1];
            }
        }
        return timeStr.length() >= 5 ? timeStr.substring(0, 5) : timeStr;
    }

    @Override
    public int getItemCount() {
        return reservationsList.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvCardId, tvCardStatus, tvCardNode, tvCardDate, tvCardEnergy, tvCardTime;
        Button btnCardAction;
        View viewStatusBar;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCardId = itemView.findViewById(R.id.tvCardId);
            tvCardStatus = itemView.findViewById(R.id.tvCardStatus);
            tvCardNode = itemView.findViewById(R.id.tvCardNode);
            tvCardDate = itemView.findViewById(R.id.tvCardDate);
            tvCardEnergy = itemView.findViewById(R.id.tvCardEnergy);
            tvCardTime = itemView.findViewById(R.id.tvCardTime);
            btnCardAction = itemView.findViewById(R.id.btnCardAction);
            viewStatusBar = itemView.findViewById(R.id.viewStatusBar);
        }
    }
}
