package com.shame.tracker.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shame.tracker.R
import com.shame.tracker.data.db.entity.Session
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionCardAdapter(
    private val onClick: (Session) -> Unit
) : ListAdapter<Session, SessionCardAdapter.ViewHolder>(SessionDiffCallback()) {

    class ViewHolder(view: View, val onClick: (Session) -> Unit) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvLaps: TextView = view.findViewById(R.id.tvLaps)
        val tvDistance: TextView = view.findViewById(R.id.tvDistance)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        var currentSession: Session? = null

        init {
            view.setOnClickListener { currentSession?.let { onClick(it) } }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session_card, parent, false)
        return ViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = getItem(position)
        holder.currentSession = session
        
        val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
        holder.tvDate.text = sdf.format(Date(session.startTimeMs))
        holder.tvLaps.text = "${session.totalLaps} Laps"
        holder.tvDistance.text = String.format(Locale.getDefault(), "Distance: %.2f km", session.totalDistanceMeters / 1000.0)
        
        val totalSecs = session.totalDurationMs / 1000
        val m = totalSecs / 60
        val s = totalSecs % 60
        holder.tvDuration.text = String.format(Locale.getDefault(), "Time: %02d:%02d", m, s)
    }
}

class SessionDiffCallback : DiffUtil.ItemCallback<Session>() {
    override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean = oldItem == newItem
}
