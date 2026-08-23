package com.ambrxsh.buzzbuddy.phase1

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MainFixContractTest {

    private val projectRoot = findProjectRoot()

    @Test
    fun numberPickerColorIsApiSafe() {
        val ext = read("app/src/main/java/com/ambrxsh/buzzbuddy/utils/NumberPickerExt.kt")
        assertTrue(ext.contains("fun NumberPicker.setPickerTextColor"))
        assertTrue(ext.contains("VERSION_CODES.Q"))
        val hits = File(projectRoot, "app/src/main/java").walkTopDown()
            .filter { it.extension == "kt" && it.name != "NumberPickerExt.kt" }
            .filter { it.readText().contains("Picker.setTextColor(") }
            .map { it.name }
            .toList()
        assertTrue("NumberPicker.setTextColor used outside compat helper: $hits", hits.isEmpty())
    }

    @Test
    fun settingsSwitchesSaveOnCheckedChange() {
        val settings = read("app/src/main/java/com/ambrxsh/buzzbuddy/fragments/SettingsFragment.kt")
        assertTrue(settings.contains("OnCheckedChangeListener"))
        assertTrue(settings.contains("bindSwitch"))
    }

    @Test
    fun editDoesNotRescheduleDisabledAlarms() {
        val edit = read("app/src/main/java/com/ambrxsh/buzzbuddy/EditAlarmActivity.kt")
        assertTrue(edit.contains("if (current.isEnabled)"))
        assertTrue(edit.contains("getAlarmByTimeExcluding"))
        assertTrue(edit.contains("updateAndWait"))
    }

    @Test
    fun alarmPlayerReadsSelectedSound() {
        val player = read("app/src/main/java/com/ambrxsh/buzzbuddy/AlarmPlayer.kt")
        assertTrue(player.contains("settings.alarmSound"))
        assertTrue(player.contains("TYPE_NOTIFICATION"))
        assertTrue(player.contains("catch (e: Exception)"))
    }

    @Test
    fun signingMaterialIsGitignored() {
        val ignore = read(".gitignore")
        assertTrue(ignore.contains("*.jks"))
        assertTrue(ignore.contains("*.pem"))
        assertTrue(ignore.contains("*.der"))
    }

    @Test
    fun restoreCoversUpdateTimezoneAndLockedBoot() {
        val manifest = read("app/src/main/AndroidManifest.xml")
        assertTrue(manifest.contains("LOCKED_BOOT_COMPLETED"))
        assertTrue(manifest.contains("USER_UNLOCKED"))
        assertTrue(manifest.contains("MY_PACKAGE_REPLACED"))
        assertTrue(manifest.contains("TIMEZONE_CHANGED"))
        val directBootAwareCount = Regex("""directBootAware="true"""").findAll(manifest).count()
        assertTrue(
            "Ring path components must be directBootAware (found $directBootAwareCount)",
            directBootAwareCount >= 4
        )
        assertTrue(manifest.contains(".AlarmReceiver"))
        assertFalse(read("app/src/main/java/com/ambrxsh/buzzbuddy/BuzzBuddyApp.kt").contains("setAlarmClock"))
        assertTrue(read("app/src/main/java/com/ambrxsh/buzzbuddy/BuzzBuddyApp.kt").contains("AlarmRescheduler"))
    }

    private fun read(relativePath: String): String {
        val file = File(projectRoot, relativePath)
        assertTrue("Missing $relativePath", file.exists())
        return file.readText()
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
