package com.ambrxsh.buzzbuddy.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.ambrxsh.buzzbuddy.model.SmartAlarm
import com.ambrxsh.buzzbuddy.repository.SmartAlarmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmartAlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SmartAlarmRepository = SmartAlarmRepository(application)
    val alarmList: LiveData<List<SmartAlarm>> = repository.getAllAlarms()

    fun getAllAlarms(): LiveData<List<SmartAlarm>> = alarmList

    suspend fun insertAndReturnId(smartAlarm: SmartAlarm): Long {
        return withContext(Dispatchers.IO) {
            repository.insertAndReturnId(smartAlarm)
        }
    }

    suspend fun restore(smartAlarm: SmartAlarm) {
        withContext(Dispatchers.IO) {
            repository.restore(smartAlarm)
        }
    }

    fun update(smartAlarm: SmartAlarm) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(smartAlarm)
    }

    suspend fun updateAndWait(smartAlarm: SmartAlarm) {
        withContext(Dispatchers.IO) {
            repository.update(smartAlarm)
        }
    }

    fun getAlarmById(alarmId: Int): LiveData<SmartAlarm?> {
        return repository.getAlarmById(alarmId)
    }

    fun delete(smartAlarm: SmartAlarm) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(smartAlarm)
    }

    suspend fun getAlarmByTime(hour: Int, minute: Int): SmartAlarm? {
        return withContext(Dispatchers.IO) {
            repository.getAlarmByTime(hour, minute)
        }
    }

    suspend fun getAlarmByTimeExcluding(hour: Int, minute: Int, excludeId: Int): SmartAlarm? {
        return withContext(Dispatchers.IO) {
            repository.getAlarmByTimeExcluding(hour, minute, excludeId)
        }
    }
}
