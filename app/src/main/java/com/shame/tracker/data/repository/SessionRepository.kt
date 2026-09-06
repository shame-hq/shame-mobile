package com.shame.tracker.data.repository

import com.shame.tracker.data.db.dao.GpsPointDao
import com.shame.tracker.data.db.dao.LapDao
import com.shame.tracker.data.db.dao.SessionDao
import com.shame.tracker.data.db.dao.SessionWithLapsDao
import com.shame.tracker.data.db.entity.GpsPoint
import com.shame.tracker.data.db.entity.Lap
import com.shame.tracker.data.db.entity.Session
import com.shame.tracker.data.model.SessionWithLaps
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val lapDao: LapDao,
    private val gpsPointDao: GpsPointDao,
    private val sessionWithLapsDao: SessionWithLapsDao
) {
    suspend fun createSession(session: Session): Long = sessionDao.insert(session)
    suspend fun updateSession(session: Session) = sessionDao.update(session)
    suspend fun getSession(id: Long): Session? = sessionDao.getById(id)
    fun observeAllSessions(): Flow<List<Session>> = sessionDao.observeAll()
    fun observeAllSessionsWithLaps(): Flow<List<SessionWithLaps>> = sessionWithLapsDao.observeAllWithLaps()
    fun observeSessionWithLaps(sessionId: Long): Flow<SessionWithLaps?> = sessionWithLapsDao.observeWithLaps(sessionId)
    suspend fun getSessionWithLaps(sessionId: Long): SessionWithLaps? = sessionWithLapsDao.getWithLaps(sessionId)
    suspend fun getLatestSession(): Session? = sessionDao.getLatestCompleted()
    suspend fun getUnsyncedSessions(): List<Session> = sessionDao.getUnsynced()
    suspend fun markSessionSynced(id: Long) = sessionDao.markSynced(id)
    suspend fun insertLap(lap: Lap): Long = lapDao.insert(lap)
    suspend fun updateLap(lap: Lap) = lapDao.update(lap)
    suspend fun getActiveLap(sessionId: Long): Lap? = lapDao.getActiveLap(sessionId)
    fun observeLaps(sessionId: Long): Flow<List<Lap>> = lapDao.observeBySession(sessionId)
    suspend fun getLaps(sessionId: Long): List<Lap> = lapDao.getBySession(sessionId)
    suspend fun insertGpsPoint(point: GpsPoint) = gpsPointDao.insert(point)
    suspend fun getGpsPoints(sessionId: Long): List<GpsPoint> = gpsPointDao.getBySession(sessionId)
    suspend fun getGpsPointsForLap(lapId: Long): List<GpsPoint> = gpsPointDao.getByLap(lapId)
    suspend fun getFastestLapMs(): Long? = sessionDao.getFastestLapMs()
    suspend fun getMostLapsSession(): Session? = sessionDao.getMostLapsSession()
    suspend fun getLongestDistanceSession(): Session? = sessionDao.getLongestDistanceSession()
    suspend fun getBestPaceSession(): Session? = sessionDao.getBestPaceSession()
    suspend fun getCompletedSessionCount(): Int = sessionDao.getCompletedCount()
    suspend fun getLastRunTimeMs(): Long? = sessionDao.getLastRunTimeMs()
    suspend fun getUnsyncedLaps(): List<Lap> = lapDao.getUnsyncedLaps()
    suspend fun getUnsyncedGpsPoints(): List<GpsPoint> = gpsPointDao.getUnsyncedPoints()
    suspend fun getAllSessionsForExport(): List<Session> = sessionDao.getAll()
}
