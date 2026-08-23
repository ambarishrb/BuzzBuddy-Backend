package com.ambrxsh.buzzbuddy.phase2

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Phase2BuildSecurityTest {

    private val projectRoot = findProjectRoot()

    @Test
    fun releaseMinifyAndShrinkAreEnabled() {
        val gradle = read("app/build.gradle.kts")
        assertTrue("Release minify must be enabled", gradle.contains("isMinifyEnabled = true"))
        assertTrue("Release resource shrinking must be enabled", gradle.contains("isShrinkResources = true"))
        assertTrue("Release must use ProGuard rules", gradle.contains("proguard-rules.pro"))
    }

    @Test
    fun dependenciesAreCentralizedInVersionCatalog() {
        val gradle = read("app/build.gradle.kts")
        val hardcoded = Regex("""implementation\s*\(\s*["'][^"']+:[^"']+:[^"']+["']""")
        assertTrue(
            "app/build.gradle.kts still has hardcoded dependency versions:\n${hardcoded.findAll(gradle).joinToString("\n") { it.value }}",
            hardcoded.findAll(gradle).none()
        )
        assertFalse("material must not be declared twice with raw coordinates", gradle.contains("com.google.android.material:material:"))
        assertFalse("gson must not be declared with a raw version", gradle.contains("com.google.code.gson:gson:"))
        assertFalse("core-ktx must not be declared with a raw version", gradle.contains("androidx.core:core-ktx:"))
    }

    @Test
    fun versionCatalogContainsRequiredLibraries() {
        val catalog = read("gradle/libs.versions.toml")
        listOf("gson", "material", "androidx-core-ktx", "androidx-room-runtime", "timber").forEach { alias ->
            assertTrue("libs.versions.toml missing $alias", catalog.contains(alias))
        }
    }

    @Test
    fun restrictedAndUnusedPermissionsAreRemoved() {
        val manifest = read("app/src/main/AndroidManifest.xml")
        listOf(
            "USE_EXACT_ALARM",
            "BLUETOOTH_SCAN",
            "FOREGROUND_SERVICE_CONNECTED_DEVICE"
        ).forEach { permission ->
            assertFalse("$permission must not be in the manifest", manifest.contains(permission))
        }
        assertTrue("SCHEDULE_EXACT_ALARM is required", manifest.contains("SCHEDULE_EXACT_ALARM"))
    }

    @Test
    fun proguardKeepsRoomAndGson() {
        val rules = read("app/proguard-rules.pro")
        assertTrue("Room entity keep rule missing", rules.contains("@androidx.room.Entity"))
        assertTrue("Gson / model keep rule missing", rules.contains("com.ambrxsh.buzzbuddy.model"))
        assertTrue("AlarmReceiver keep rule missing", rules.contains("AlarmReceiver"))
    }

    private fun read(relativePath: String): String {
        val file = File(projectRoot, relativePath)
        assertTrue("Missing $relativePath at ${file.absolutePath}", file.exists())
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
