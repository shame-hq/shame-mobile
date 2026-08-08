package com.shame.tracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
    private List<ShameEntry> logs;

    public LogAdapter(List<ShameEntry> logs) {
        this.logs = logs;
    }

    public void setLogs(List<ShameEntry> logs) {
        this.logs = logs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        ShameEntry log = logs.get(position);
        holder.timestampText.setText(log.getLoggedAt());
        holder.locationText.setText("Location: " + (log.getLocation() != null ? log.getLocation() : "Unknown"));
        holder.triggerText.setText("Trigger: " + (log.getTriggerMood() != null ? log.getTriggerMood() : "Not specified"));
        holder.notesText.setText("Notes: " + (log.getNotes() != null ? log.getNotes() : ""));
    }

    @Override
    public int getItemCount() {
        return logs != null ? logs.size() : 0;
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView timestampText, locationText, triggerText, notesText;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            timestampText = itemView.findViewById(R.id.timestampText);
            locationText = itemView.findViewById(R.id.locationText);
            triggerText = itemView.findViewById(R.id.triggerText);
            notesText = itemView.findViewById(R.id.notesText);
        }
    }
}
