package com.shame.tracker.ui.session

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shame.tracker.R
import com.shame.tracker.data.db.entity.Lap
import com.shame.tracker.util.LapColors
import java.util.Locale

class LapSplitAdapter : ListAdapter<Lap, LapSplitAdapter.ViewHolder>(LapDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewLapColor: View     = view.findViewById(R.id.viewLapColor)
        val tvLapNum: TextView     = view.findViewById(R.id.tvLapNum)
        val tvLapTime: TextView    = view.findViewById(R.id.tvLapTime)
        val tvLapDist: TextView    = view.findViewById(R.id.tvLapDist)
        val tvLapPace: TextView    = view.findViewById(R.id.tvLapPace)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lap_split, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lap = getItem(position)

        // Color dot
        val color = LapColors.forLapNumber(lap.lapNumber)
        (holder.viewLapColor.background as? GradientDrawable)?.setColor(color)
            ?: run {
                val circle = GradientDrawable()
                circle.shape = GradientDrawable.OVAL
                circle.setColor(color)
                holder.viewLapColor.background = circle
            }

        holder.tvLapNum.text = "Lap ${lap.lapNumber}"
        holder.tvLapNum.setTextColor(color)

        val totalSecs = lap.durationMs / 1000
        holder.tvLapTime.text = if (lap.endTimeMs != null) {
            String.format(Locale.getDefault(), "%02d:%02d", totalSecs / 60, totalSecs % 60)
        } else {
            "--:--"
        }

        holder.tvLapDist.text = String.format(Locale.getDefault(), "%.2f km", lap.distanceMeters / 1000.0)

        // avgPaceSecPerKm is stored in sec/km; show as m:ss /km
        val paceSec = lap.avgPaceSecPerKm.toLong()
        holder.tvLapPace.text = if (paceSec > 0) {
            String.format(Locale.getDefault(), "%d:%02d /km", paceSec / 60, paceSec % 60)
        } else {
            "-- /km"
        }
    }
}

class LapDiffCallback : DiffUtil.ItemCallback<Lap>() {
    override fun areItemsTheSame(oldItem: Lap, newItem: Lap): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Lap, newItem: Lap): Boolean = oldItem == newItem
}
