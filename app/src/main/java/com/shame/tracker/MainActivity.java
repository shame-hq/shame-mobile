package com.shame.tracker;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB4cHBpeXd2enVrZWFvaGlxbGZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njg5NDI1NzcsImV4cCI6MjA4NDUxODU3N30.7PO9kqY3e0C276TaawihWLJsHfbPzwNgsmroH6JZJBQ";
    private TextView statusTextView;
    private RecyclerView logsRecyclerView;
    private LogAdapter adapter;
    private List<ShameEntry> logsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.statusTextView);
        logsRecyclerView = findViewById(R.id.logsRecyclerView);
        FloatingActionButton addLogFab = findViewById(R.id.addLogFab);

        logsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LogAdapter(logsList);
        logsRecyclerView.setAdapter(adapter);

        addLogFab.setOnClickListener(v -> showAddLogDialog());

        fetchLogs();

        // Check for app updates
        new UpdateManager(this).checkForUpdates();
    }

    private void fetchLogs() {
        statusTextView.setText("Fetching logs...");
        ApiService apiService = SupabaseClient.getApiService();
        Call<List<ShameEntry>> call = apiService.getLogs(SUPABASE_KEY, "Bearer " + SUPABASE_KEY, "*");

        call.enqueue(new Callback<List<ShameEntry>>() {
            @Override
            public void onResponse(Call<List<ShameEntry>> call, Response<List<ShameEntry>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    logsList.clear();
                    logsList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    statusTextView.setText("Successfully fetched " + logsList.size() + " entries.");
                } else {
                    statusTextView.setText("Failed to fetch data: " + response.code());
                    Toast.makeText(MainActivity.this, "Error: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ShameEntry>> call, Throwable t) {
                statusTextView.setText("Error: " + t.getMessage());
                Toast.makeText(MainActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddLogDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_log, null);
        EditText editLocation = dialogView.findViewById(R.id.editLocation);
        EditText editTrigger = dialogView.findViewById(R.id.editTrigger);
        EditText editNotes = dialogView.findViewById(R.id.editNotes);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String location = editLocation.getText().toString();
                    String trigger = editTrigger.getText().toString();
                    String notes = editNotes.getText().toString();
                    addLog(location, trigger, notes);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addLog(String location, String trigger, String notes) {
        // Prepare timestamp in ISO 8601 format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = sdf.format(new Date());

        ShameEntry newEntry = new ShameEntry(timestamp, location, trigger, notes);

        ApiService apiService = SupabaseClient.getApiService();
        Call<Void> call = apiService.addLog(
                SUPABASE_KEY,
                "Bearer " + SUPABASE_KEY,
                "application/json",
                "return=minimal",
                newEntry);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Log added!", Toast.LENGTH_SHORT).show();
                    fetchLogs(); // Refresh list
                } else {
                    Toast.makeText(MainActivity.this, "Failed to add log: " + response.code(), Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error adding log: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
