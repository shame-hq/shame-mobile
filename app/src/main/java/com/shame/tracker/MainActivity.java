package com.shame.tracker;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB4cHBpeXd2enVrZWFvaGlxbGZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njg5NDI1NzcsImV4cCI6MjA4NDUxODU3N30.7PO9kqY3e0C276TaawihWLJsHfbPzwNgsmroH6JZJBQ";
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.statusTextView);
        fetchLogs();
    }

    private void fetchLogs() {
        ApiService apiService = SupabaseClient.getApiService();
        Call<List<ShameEntry>> call = apiService.getLogs(SUPABASE_KEY, "Bearer " + SUPABASE_KEY, "*");

        call.enqueue(new Callback<List<ShameEntry>>() {
            @Override
            public void onResponse(Call<List<ShameEntry>> call, Response<List<ShameEntry>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int count = response.body().size();
                    statusTextView.setText("Successfully fetched " + count + " entries.");
                } else {
                    statusTextView.setText("Failed to fetch data: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<ShameEntry>> call, Throwable t) {
                statusTextView.setText("Error: " + t.getMessage());
            }
        });
    }
}
