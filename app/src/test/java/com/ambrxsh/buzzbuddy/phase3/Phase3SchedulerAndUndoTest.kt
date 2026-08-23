package com.ambrxsh.buzzbuddy.phase3

import com.ambrxsh.buzzbuddy.scheduler.AlarmScheduleMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit

class Phase3SchedulerAndUndoTest {

    @Test
    fun snoozeUsesDifferentRequestCodeThanDailyAlarm() {
        val alarmId = 42
        val daily = AlarmScheduleMath.requestCode(alarmId, isSnooze = false)
        val snooze = AlarmScheduleMath.requestCode(alarmId, isSnooze = true)
        assertEquals(alarmId, daily)
        assertEquals(alarmId + AlarmScheduleMath.SNOOZE_REQUEST_OFFSET, snooze)
        assertNotEquals(daily, snooze)
    }

    @Test
    fun nextTriggerIsTodayWhenTimeIsInTheFuture() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val trigger = AlarmScheduleMath.nextTriggerMillis(9, 30, now)
        val calendar = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(9, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calendar.get(Calendar.MINUTE))
        assertEquals(0, calendar.get(Calendar.SECOND))
        assertTrue(trigger > now)
        assertTrue(trigger - now < TimeUnit.DAYS.toMillis(1))
    }

    @Test
    fun nextTriggerRollsToTomorrowWhenTimeAlreadyPassed() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val trigger = AlarmScheduleMath.nextTriggerMillis(9, 0, now)
        assertTrue("Passed times must schedule tomorrow, not immediately", trigger > now)
        assertTrue(trigger - now >= TimeUnit.HOURS.toMillis(20))
    }

    @Test
    fun snoozeTriggerIsExactMinutesFromNowNotRoundedToClockMinute() {
        val now = 1_700_000_123_456L
        val trigger = AlarmScheduleMath.snoozeTriggerMillis(10, now)
        assertEquals(now + TimeUnit.MINUTES.toMillis(10), trigger)
        assertEquals(now + 60_000L, AlarmScheduleMath.snoozeTriggerMillis(0, now))
    }

    @Test
    fun midnightAndLastMinuteOfDayStayValid() {
        val evening = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val midnight = AlarmScheduleMath.nextTriggerMillis(0, 0, evening)
        val late = AlarmScheduleMath.nextTriggerMillis(23, 59, evening)
        assertTrue(midnight > evening)
        assertTrue(late > evening)
        val midnightCal = Calendar.getInstance().apply { timeInMillis = midnight }
        assertEquals(0, midnightCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, midnightCal.get(Calendar.MINUTE))
    }

    @Test
    fun schedulingIsCentralizedInBuzzBuddyAlarmScheduler() {
        val root = findProjectRoot()
        val sources = File(root, "app/src/main/java").walkTopDown()
            .filter { it.extension == "kt" }
            .toList()

        val duplicatedScheduler = sources.filter { file ->
            file.name != "BuzzBuddyAlarmScheduler.kt" &&
                file.readText().contains("setAlarmClock")
        }
        assertTrue(
            "setAlarmClock should only live in BuzzBuddyAlarmScheduler, also found in ${duplicatedScheduler.map { it.name }}",
            duplicatedScheduler.isEmpty()
        )

        val callers = sources.filter { file ->
            file.readText().contains("BuzzBuddyAlarmScheduler")
        }.map { it.name }
        assertTrue("SetAlarmPage must use the shared scheduler", callers.contains("SetAlarmPage.kt"))
        assertTrue("BootReceiver must use the shared scheduler", callers.contains("BootReceiver.kt"))
        assertTrue("EditAlarmActivity must use the shared scheduler", callers.contains("EditAlarmActivity.kt"))
    }

    @Test
    fun undoRestoresInsteadOfInsertingANewId() {
        val setAlarmPage = File(findProjectRoot(), "app/src/main/java/com/ambrxsh/buzzbuddy/fragments/SetAlarmPage.kt").readText()
        assertTrue("Undo must call restore()", setAlarmPage.contains("smartAlarmViewModel.restore(alarmToDelete)"))
        assertFalse(
            "Undo must not assign a new ID from insertAndReturnId",
            setAlarmPage.contains("insertAndReturnId(alarmToDelete)")
        )
        val repository = File(findProjectRoot(), "app/src/main/java/com/ambrxsh/buzzbuddy/repository/SmartAlarmRepository.kt").readText()
        assertTrue("Repository must expose restore()", repository.contains("suspend fun restore"))
    }

    private fun assertFalse(message: String, condition: Boolean) {
        org.junit.Assert.assertFalse(message, condition)
    }

    private fun findProjectRoot(): File {
        var dir = File("").absoluteFile
        repeat(6) {
            if (File(dir, "settings.gradle.kts").exists() && File(dir, "app").isDirectory) {
                return dir
            }
            dir = dir.parentFile ?: return@repeat
        }
        throw IllegalStateException("Could not find project root from ${File("").absoluteFile}")
    }
}
