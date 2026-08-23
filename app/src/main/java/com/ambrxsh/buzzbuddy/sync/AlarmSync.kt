package com.ambrxsh.buzzbuddy.sync

import android.content.Context
import android.util.Log
import com.ambrxsh.buzzbuddy.BuzzBuddyApp
import com.ambrxsh.buzzbuddy.clients.AlarmBackendService
import com.ambrxsh.buzzbuddy.model.SmartAlarm
import com.ambrxsh.buzzbuddy.room.SmartAlarmsDatabase
import com.ambrxsh.buzzbuddy.scheduler.BuzzBuddyAlarmScheduler

object AlarmSync {

    suspend fun restoreFromServer(context: Context, app: BuzzBuddyApp) {
        val api = app.retrofit.create(AlarmBackendService::class.java)
        val dao = SmartAlarmsDatabase.getDatabase(context).smartAlarmDao()
        val scheduler = BuzzBuddyAlarmScheduler(context)
        val remote = try {
            api.listAlarms()
        } catch (e: Exception) {
            Log.w(TAG, "restore fetch failed", e)
            return
        }
        val remoteIds = remote.mapNotNull { it.id }.toSet()
        for (local in dao.getAllAlarmsSync()) {
            val serverId = local.serverId
            if (serverId != null && serverId !in remoteIds) {
                scheduler.cancel(local.alarmId)
                dao.delete(local)
            }
        }
        for (dto in remote) {
            val serverId = dto.id ?: continue
            val existing = dao.getByServerId(serverId)
            val mapped = SmartAlarm(
                alarmTitle = dto.title,
                alarmTime_hour = dto.hour,
                alarmTime_minute = dto.minute,
                isEnabled = dto.enabled,
                alarmId = existing?.alarmId ?: 0,
                serverId = serverId
            )
            if (existing == null) {
                dao.insert(mapped)
            } else {
                dao.update(mapped)
            }
        }
        for (local in dao.getAllAlarmsSync()) {
            if (local.serverId != null) continue
            try {
                val created = api.createAlarm(
                    com.ambrxsh.buzzbuddy.dtos.AlarmDto(
                        id = null,
                        title = local.alarmTitle,
                        hour = local.alarmTime_hour,
                        minute = local.alarmTime_minute,
                        enabled = local.isEnabled
                    )
                )
                local.serverId = created.id
                dao.update(local)
            } catch (e: Exception) {
                Log.w(TAG, "push unsynced alarm failed", e)
            }
        }
        for (alarm in dao.getAllAlarmsSync()) {
            scheduler.cancel(alarm.alarmId)
            if (alarm.isEnabled) {
                scheduler.schedule(alarm.alarmId, alarm.alarmTime_hour, alarm.alarmTime_minute)
            }
        }
    }

    private const val TAG = "AlarmSync"
}
