package com.ambrxsh.buzzbuddy.phase1

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Phase1CriticalStabilizationTest {

    private val projectRoot = findProjectRoot()
    private val mainJava = File(projectRoot, "app/src/main/java")

    @Test
    fun usesScheduleExactAlarmInsteadOfUseExactAlarm() {
        val manifest = File(projectRoot, "app/src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android.permission.SCHEDULE_EXACT_ALARM"))
        assertFalse(manifest.contains("USE_EXACT_ALARM"))
    }

    @Test
    fun schedulerChecksPermissionAndCatchesSecurityException() {
        val scheduler = File(
            projectRoot,
            "app/src/main/java/com/ambrxsh/buzzbuddy/scheduler/BuzzBuddyAlarmScheduler.kt"
        ).readText()
        assertTrue(scheduler.contains("canScheduleExactAlarms()"))
        assertTrue(scheduler.contains("SecurityException"))
        assertTrue(scheduler.contains("ACTION_REQUEST_SCHEDULE_EXACT_ALARM"))
    }

    @Test
    fun missingExactAlarmSendsUserToSettings() {
        val helper = File(
            projectRoot,
            "app/src/main/java/com/ambrxsh/buzzbuddy/utils/AlarmPermissionHelper.kt"
        ).readText()
        assertTrue(helper.contains("requestExactAlarmPermission"))
        assertTrue(helper.contains("openExactAlarmSettings"))
    }

    @Test
    fun fragmentsDoNotUseDeprecatedLaunchWhenStarted() {
        val hits = mainJava.walkTopDown()
            .filter { it.extension == "kt" }
            .filter { it.readText().contains("launchWhenStarted") }
            .map { it.name }
            .toList()
        assertTrue("Deprecated launchWhenStarted still used in $hits", hits.isEmpty())
    }

    @Test
    fun alarmListUsesLifecycleAwareObserver() {
        val setAlarmPage = File(
            projectRoot,
            "app/src/main/java/com/ambrxsh/buzzbuddy/fragments/SetAlarmPage.kt"
        ).readText()
        assertTrue(setAlarmPage.contains("observe(viewLifecycleOwner)"))
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
