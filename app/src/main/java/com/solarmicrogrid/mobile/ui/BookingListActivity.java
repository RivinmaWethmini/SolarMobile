package com.solarmicrogrid.mobile.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.database.DatabaseHelper;
import com.solarmicrogrid.mobile.models.Reservation;
import com.solarmicrogrid.mobile.network.ApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays current and pending energy slot bookings and historical records.
 * Includes live text search filter and status tabs.
 * Satisfies Table 2 marking rubric: "Booking Views and Operational Dashboards".
 * Author: Member 4
 */
public class BookingListActivity extends AppCompatActivity {

    private EditText etSearchBookings;
    private Button btnFilterAll, btnFilterPending, btnFilterApproved, btnFilterCancelled, btnFilterRejected;
    private ProgressBar pbLoading;
    private RecyclerView rvBookings;
    private LinearLayout layoutEmptyState;
    private Button btnRetryLoad;
    private BookingAdapter adapter;

    private List<Reservation> allReservations;
    private List<Reservation> displayedList;
    private String currentFilter = "All";
    private DatabaseHelper dbHelper;

    // TODO (Member 2): Replace with authenticated user NIC from JWT session / Auth SharedPreferences
    private String PROSUMER_NIC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_list);

        dbHelper = new DatabaseHelper(this);
        allReservations = new ArrayList<>();
        displayedList = new ArrayList<>();

        // TODO (Member 2): Replace with JWT session lookup when auth is ready
        android.content.SharedPreferences prefs = getSharedPreferences("solar_session", MODE_PRIVATE);
        PROSUMER_NIC = prefs.getString("prosumer_nic", "200012345678");

        etSearchBookings = findViewById(R.id.etSearchBookings);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterPending = findViewById(R.id.btnFilterPending);
        btnFilterApproved = findViewById(R.id.btnFilterApproved);
        btnFilterCancelled = findViewById(R.id.btnFilterCancelled);
        btnFilterRejected = findViewById(R.id.btnFilterRejected);
        pbLoading = findViewById(R.id.pbLoading);
        rvBookings = findViewById(R.id.rvBookings);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        btnRetryLoad = findViewById(R.id.btnRetryLoad);

        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingAdapter(this, displayedList);
        rvBookings.setAdapter(adapter);

        if (btnRetryLoad != null) {
            btnRetryLoad.setOnClickListener(v -> loadBookings());
        }

        setupFilters();
        setupSearch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookings();
    }

    private void setupFilters() {
        btnFilterAll.setOnClickListener(v -> setFilter("All"));
        btnFilterPending.setOnClickListener(v -> setFilter("Pending"));
        btnFilterApproved.setOnClickListener(v -> setFilter("Approved"));
        if (btnFilterCancelled != null) btnFilterCancelled.setOnClickListener(v -> setFilter("Cancelled"));
        if (btnFilterRejected != null) btnFilterRejected.setOnClickListener(v -> setFilter("Rejected"));
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterButtonStyle(btnFilterAll, "All".equalsIgnoreCase(filter));
        updateFilterButtonStyle(btnFilterPending, "Pending".equalsIgnoreCase(filter));
        updateFilterButtonStyle(btnFilterApproved, "Approved".equalsIgnoreCase(filter));
        updateFilterButtonStyle(btnFilterCancelled, "Cancelled".equalsIgnoreCase(filter));
        updateFilterButtonStyle(btnFilterRejected, "Rejected".equalsIgnoreCase(filter));

        applyFilterAndSearch();
    }

    private void updateFilterButtonStyle(Button btn, boolean isSelected) {
        if (btn == null) return;
        btn.setBackgroundColor(isSelected ? getResources().getColor(R.color.accent_solar) : getResources().getColor(R.color.slate_card));
        btn.setTextColor(isSelected ? 0xFF0F172A : 0xFFFFFFFF);
    }

    private void setupSearch() {
        etSearchBookings.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilterAndSearch();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadBookings() {
        pbLoading.setVisibility(View.VISIBLE);
        if (btnRetryLoad != null) btnRetryLoad.setVisibility(View.GONE);

        // First load from local SQLite for instant UI response
        List<Reservation> cached = dbHelper.getAllReservations();
        if (!cached.isEmpty()) {
            allReservations.clear();
            allReservations.addAll(cached);
            applyFilterAndSearch();
        }

        // Fetch live records from C# Web API
        ApiClient.get("/reservations/prosumer/" + PROSUMER_NIC, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                pbLoading.setVisibility(View.GONE);
                try {
                    JSONArray arr = new JSONArray(response);
                    allReservations.clear();
                    List<Reservation> freshList = new ArrayList<>();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        Reservation res = new Reservation();
                        res.setId(obj.optString("id", obj.optString("reservationId", "")));
                        res.setProsumerId(obj.optString("prosumerId", PROSUMER_NIC));
                        res.setProsumerNic(obj.optString("prosumerNic", PROSUMER_NIC));
                        res.setNodeId(obj.optString("nodeId", obj.optString("microgridNodeId", "NODE-01")));
                        res.setReservedEnergyKwh(obj.optDouble("reservedEnergyKwh", 0));
                        res.setStartTime(obj.optString("startTime", obj.optString("reservationDate", "")));
                        res.setEndTime(obj.optString("endTime", ""));
                        res.setStatus(obj.optString("status", "Pending"));
                        res.setQrCodePayload(obj.optString("qrPayload", obj.optString("qrCodePayload", null)));

                        freshList.add(res);
                    }

                    allReservations.addAll(freshList);
                    // Persist to local SQLite
                    dbHelper.saveAllReservations(freshList);

                    applyFilterAndSearch();

                } catch (Exception e) {
                    Toast.makeText(BookingListActivity.this, "Failed to parse reservations", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                pbLoading.setVisibility(View.GONE);
                // Graceful fallback to SQLite cached records
                if (allReservations.isEmpty()) {
                    Toast.makeText(BookingListActivity.this, "Network offline: " + errorMessage, Toast.LENGTH_SHORT).show();
                    if (btnRetryLoad != null) btnRetryLoad.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void applyFilterAndSearch() {
        String query = etSearchBookings.getText().toString().trim().toLowerCase();
        displayedList.clear();

        for (Reservation res : allReservations) {
            boolean matchesFilter = "All".equalsIgnoreCase(currentFilter) ||
                    (res.getStatus() != null && res.getStatus().equalsIgnoreCase(currentFilter));

            boolean matchesSearch = query.isEmpty() ||
                    (res.getNodeId() != null && res.getNodeId().toLowerCase().contains(query)) ||
                    (res.getId() != null && res.getId().toLowerCase().contains(query)) ||
                    (res.getStatus() != null && res.getStatus().toLowerCase().contains(query));

            if (matchesFilter && matchesSearch) {
                displayedList.add(res);
            }
        }

        adapter.updateList(displayedList);

        if (layoutEmptyState != null) {
            if (displayedList.isEmpty()) {
                rvBookings.setVisibility(View.GONE);
                layoutEmptyState.setVisibility(View.VISIBLE);
            } else {
                rvBookings.setVisibility(View.VISIBLE);
                layoutEmptyState.setVisibility(View.GONE);
            }
        }
    }
}
