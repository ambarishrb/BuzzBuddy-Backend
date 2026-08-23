package com.ambrxsh.buzzbuddy.phase4

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Phase4CodeQualityTest {

    private val projectRoot = findProjectRoot()

    @Test
    fun mainSourcesDoNotUsePrintlnOrPrintStackTrace() {
        val hits = File(projectRoot, "app/src/main/java").walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("//")) return@mapIndexedNotNull null
                    if (Regex("""\b(println|print)\s*\(""").containsMatchIn(line) ||
                        line.contains("printStackTrace()")
                    ) {
                        "${file.name}:${index + 1}: $trimmed"
                    } else {
                        null
                    }
                }
            }
            .toList()
        assertTrue("Debug prints still present:\n${hits.joinToString("\n")}", hits.isEmpty())
    }

    @Test
    fun timberIsReleaseSafe() {
        val app = File(projectRoot, "app/src/main/java/com/ambrxsh/buzzbuddy/BuzzBuddyApp.kt").readText()
        assertTrue(app.contains("Timber.plant(Timber.DebugTree())"))
        assertTrue(app.contains("BuildConfig.DEBUG"))
    }

    @Test
    fun userFacingCopyLivesInStringsXml() {
        val strings = File(projectRoot, "app/src/main/res/values/strings.xml").readText()
        listOf(
            "alarm_deleted",
            "alarm_already_set",
            "settings_title",
            "snooze",
            "dismiss",
            "set_alarm_title"
        ).forEach { key ->
            assertTrue("Missing string $key", strings.contains("name=\"$key\""))
        }
    }

    @Test
    fun layoutsDoNotHardcodeUserVisibleText() {
        val layoutDir = File(projectRoot, "app/src/main/res/layout")
        val hardcoded = layoutDir.listFiles().orEmpty()
            .filter { it.extension == "xml" }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("tools:")) return@mapIndexedNotNull null
                    val isUserText = Regex("""(android:text|android:hint|android:title|app:title|android:contentDescription)\s*=\s*"[^@?]""")
                    if (isUserText.containsMatchIn(trimmed)) {
                        "${file.name}:${index + 1}: $trimmed"
                    } else {
                        null
                    }
                }
            }
        assertTrue("Hardcoded UI strings:\n${hardcoded.joinToString("\n")}", hardcoded.isEmpty())
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
