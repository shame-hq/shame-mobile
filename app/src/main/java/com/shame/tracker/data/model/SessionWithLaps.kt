package com.shame.tracker.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.shame.tracker.data.db.entity.Lap
import com.shame.tracker.data.db.entity.Session

data class SessionWithLaps(
    @Embedded val session: Session,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val laps: List<Lap>
)
