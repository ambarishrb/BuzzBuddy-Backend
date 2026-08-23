package com.ambrxsh.buzzbuddy.phase3

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ambrxsh.buzzbuddy.model.SmartAlarm
import com.ambrxsh.buzzbuddy.room.SmartAlarmsDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase3UndoRestoreTest {

    private lateinit var database: SmartAlarmsDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, SmartAlarmsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun restoreKeepsOriginalAlarmId() = runBlocking {
        val dao = database.smartAlarmDao()
        val alarm = SmartAlarm(
            alarmTitle = "UndoTest",
            alarmTime_hour = 7,
            alarmTime_minute = 15,
            isEnabled = true
        )
        val generatedId = dao.insert(alarm).toInt()
        alarm.alarmId = generatedId

        dao.delete(alarm)
        assertNull(dao.getAlarmByTime(7, 15))

        dao.insert(alarm)
        val restored = dao.getAlarmByTime(7, 15)
        assertNotNull(restored)
        assertEquals(generatedId, restored!!.alarmId)
        assertEquals("UndoTest", restored.alarmTitle)
        assertEquals(7, restored.alarmTime_hour)
        assertEquals(15, restored.alarmTime_minute)
    }
}
