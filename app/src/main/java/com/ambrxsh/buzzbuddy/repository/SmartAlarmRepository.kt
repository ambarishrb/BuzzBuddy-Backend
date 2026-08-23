package com.ambrxsh.buzzbuddy.repository

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.ambrxsh.buzzbuddy.BuzzBuddyApp
import com.ambrxsh.buzzbuddy.clients.AlarmBackendService
import com.ambrxsh.buzzbuddy.dtos.AlarmDto
import com.ambrxsh.buzzbuddy.model.SmartAlarm
import com.ambrxsh.buzzbuddy.room.SmartAlarmsDatabase
import com.ambrxsh.buzzbuddy.room.smartAlarmDao
import com.ambrxsh.buzzbuddy.scheduler.AlarmScheduleCache
import com.ambrxsh.buzzbuddy.utils.SessionStore

class SmartAlarmRepository(application: Application) {

    private val appContext = application.applicationContext
    private val smartAlarmDao: smartAlarmDao
    private val alarmList: LiveData<List<SmartAlarm>>
    private val alarmApi: AlarmBackendService?
    private val session: SessionStore?

    init {
        val database = SmartAlarmsDatabase.getDatabase(application)
        smartAlarmDao = database.smartAlarmDao()
        alarmList = smartAlarmDao.getAllAlarms()
        val app = application as? BuzzBuddyApp
        alarmApi = app?.retrofit?.create(AlarmBackendService::class.java)
        session = app?.session
    }

    private fun canSync(): Boolean = session?.isLoggedIn() == true && alarmApi != null

    suspend fun insertAndReturnId(smartAlarm: SmartAlarm): Long {
        val id = smartAlarmDao.insert(smartAlarm)
        smartAlarm.alarmId = id.toInt()
        persistScheduleCache()
        pushCreate(smartAlarm)
        return id
    }

    suspend fun restore(smartAlarm: SmartAlarm) {
        smartAlarmDao.insert(smartAlarm)
        persistScheduleCache()
        if (smartAlarm.serverId == null) {
            pushCreate(smartAlarm)
        }
    }

    suspend fun update(smartAlarm: SmartAlarm) {
        smartAlarmDao.update(smartAlarm)
        persistScheduleCache()
        if (!canSync()) return
        val serverId = smartAlarm.serverId ?: return
        try {
            alarmApi?.updateAlarm(serverId, smartAlarm.toDto())
        } catch (e: Exception) {
            Log.w(TAG, "sync update failed", e)
        }
    }

    suspend fun delete(smartAlarm: SmartAlarm) {
        smartAlarmDao.delete(smartAlarm)
        persistScheduleCache()
        if (!canSync()) return
        val serverId = smartAlarm.serverId ?: return
        try {
            alarmApi?.deleteAlarm(serverId)
        } catch (e: Exception) {
            Log.w(TAG, "sync delete failed", e)
        }
    }

    fun getAlarmById(alarmId: Int): LiveData<SmartAlarm?> {
        return smartAlarmDao.getAlarmByIdLive(alarmId)
    }

    fun getAllAlarms(): LiveData<List<SmartAlarm>> {
        return alarmList
    }

    suspend fun getAlarmByTime(hour: Int, minute: Int): SmartAlarm? {
        return smartAlarmDao.getAlarmByTime(hour, minute)
    }

    suspend fun getAlarmByTimeExcluding(hour: Int, minute: Int, excludeId: Int): SmartAlarm? {
        return smartAlarmDao.getAlarmByTimeExcluding(hour, minute, excludeId)
    }

    private suspend fun pushCreate(alarm: SmartAlarm) {
        if (!canSync()) return
        try {
            val created = alarmApi?.createAlarm(alarm.toDto()) ?: return
            alarm.serverId = created.id
            smartAlarmDao.update(alarm)
        } catch (e: Exception) {
            Log.w(TAG, "sync create failed", e)
        }
    }

    private fun persistScheduleCache() {
        AlarmScheduleCache.save(
            appContext,
            smartAlarmDao.getAllAlarmsSync().filter { it.isEnabled }
        )
    }

    private fun SmartAlarm.toDto() = AlarmDto(
        id = serverId,
        title = alarmTitle,
        hour = alarmTime_hour,
        minute = alarmTime_minute,
        enabled = isEnabled
    )

    companion object {
        private const val TAG = "SmartAlarmRepository"
    }
}
