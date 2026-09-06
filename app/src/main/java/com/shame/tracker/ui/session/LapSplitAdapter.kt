package com.shame.tracker.ui.session

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shame.tracker.R
import com.shame.tracker.data.db.entity.Lap
import java.util.Locale

class LapSplitAdapter : ListAdapter<Lap, LapSplitAdapter.ViewHolder>(LapDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLapNum: TextView = view.findViewById(R.id.tvLapNum)
        val tvLapTime: TextView = view.findViewById(R.id.tvLapTime)
        val tvLapDist: TextView = view.findViewById(R.id.tvLapDist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lap_split, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lap = getItem(position)
        holder.tvLapNum.text = "Lap ${lap.lapNumber}"
        
        val totalSecs = lap.durationMs / 1000
        holder.tvLapTime.text = String.format(Locale.getDefault(), "%02d:%02d", totalSecs / 60, totalSecs % 60)
        holder.tvLapDist.text = String.format(Locale.getDefault(), "%.2f km", lap.distanceMeters / 1000.0)
    }
}

class LapDiffCallback : DiffUtil.ItemCallback<Lap>() {
    override fun areItemsTheSame(oldItem: Lap, newItem: Lap): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Lap, newItem: Lap): Boolean = oldItem == newItem
}
