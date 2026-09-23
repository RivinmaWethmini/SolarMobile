package com.solarmicrogrid.mobile.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.solarmicrogrid.mobile.R;
import com.solarmicrogrid.mobile.models.MicrogridNode;
import com.solarmicrogrid.mobile.network.ApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NodeListActivity extends AppCompatActivity {

    private ProgressBar pbNodeLoading;
    private LinearLayout layoutNodeEmpty;
    private TextView tvNodeEmpty;
    private TextView tvNodeEmptyDescription;
    private RecyclerView rvNodes;
    private AppCompatButton btnRetryNodes;
    private NodeAdapter adapter;

    private final List<MicrogridNode> nodeList =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_node_list);

        getWindow().setStatusBarColor(
                ContextCompat.getColor(
                        this,
                        R.color.slate_card
                )
        );

        TextView btnBackNodes = findViewById(
                R.id.btnBackNodes
        );

        pbNodeLoading = findViewById(
                R.id.pbNodeLoading
        );

        layoutNodeEmpty = findViewById(
                R.id.layoutNodeEmpty
        );

        tvNodeEmpty = findViewById(
                R.id.tvNodeEmpty
        );

        tvNodeEmptyDescription = findViewById(
                R.id.tvNodeEmptyDescription
        );

        rvNodes = findViewById(
                R.id.rvNodes
        );

        btnRetryNodes = findViewById(
                R.id.btnRetryNodes
        );

        rvNodes.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvNodes.setHasFixedSize(true);

        adapter = new NodeAdapter(
                this,
                nodeList
        );

        rvNodes.setAdapter(adapter);

        btnBackNodes.setOnClickListener(
                view -> finish()
        );

        btnRetryNodes.setOnClickListener(
                view -> loadActiveNodes()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadActiveNodes();
    }

    private void loadActiveNodes() {
        pbNodeLoading.setVisibility(View.VISIBLE);
        layoutNodeEmpty.setVisibility(View.GONE);
        rvNodes.setVisibility(View.GONE);

        ApiClient.get(
                "/nodes",
                new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        pbNodeLoading.setVisibility(View.GONE);

                        try {
                            JSONArray array =
                                    new JSONArray(response);

                            nodeList.clear();

                            for (
                                    int index = 0;
                                    index < array.length();
                                    index++
                            ) {
                                JSONObject object =
                                        array.getJSONObject(index);

                                String status =
                                        object.optString(
                                                "status",
                                                "Inactive"
                                        );

                                if (
                                        !"Active".equalsIgnoreCase(
                                                status
                                        )
                                ) {
                                    continue;
                                }

                                String schedule =
                                        object.optString(
                                                "schedule",
                                                ""
                                        );

                                if (
                                        "null".equalsIgnoreCase(
                                                schedule
                                        )
                                ) {
                                    schedule = "";
                                }

                                MicrogridNode node =
                                        new MicrogridNode(
                                                object.optString(
                                                        "id",
                                                        ""
                                                ),
                                                object.optString(
                                                        "name",
                                                        "Unnamed Node"
                                                ),
                                                object.optDouble(
                                                        "latitude",
                                                        0
                                                ),
                                                object.optDouble(
                                                        "longitude",
                                                        0
                                                ),
                                                object.optDouble(
                                                        "capacityKWh",
                                                        0
                                                ),
                                                object.optInt(
                                                        "batterySlots",
                                                        0
                                                ),
                                                schedule,
                                                status
                                        );

                                nodeList.add(node);
                            }

                            adapter.updateList(nodeList);

                            if (nodeList.isEmpty()) {
                                showEmptyState(
                                        "No active nodes found",
                                        "Active solar nodes will appear here."
                                );
                            } else {
                                layoutNodeEmpty.setVisibility(
                                        View.GONE
                                );

                                rvNodes.setVisibility(
                                        View.VISIBLE
                                );
                            }
                        } catch (Exception exception) {
                            showEmptyState(
                                    "Unable to display nodes",
                                    "The server returned an invalid response."
                            );

                            Toast.makeText(
                                    NodeListActivity.this,
                                    "Failed to parse node information",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        pbNodeLoading.setVisibility(View.GONE);

                        showEmptyState(
                                "Unable to load nodes",
                                "Check the API connection and try again."
                        );

                        Toast.makeText(
                                NodeListActivity.this,
                                "Network error: " + errorMessage,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void showEmptyState(
            String title,
            String description
    ) {
        rvNodes.setVisibility(View.GONE);
        layoutNodeEmpty.setVisibility(View.VISIBLE);
        tvNodeEmpty.setText(title);
        tvNodeEmptyDescription.setText(description);
    }
}