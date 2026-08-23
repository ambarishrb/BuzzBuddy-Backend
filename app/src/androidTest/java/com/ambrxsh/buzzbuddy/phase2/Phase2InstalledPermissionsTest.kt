package com.ambrxsh.buzzbuddy.phase2

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase2InstalledPermissionsTest {

    @Test
    fun installedApkDoesNotDeclareRemovedPermissions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requested = info.requestedPermissions?.toSet().orEmpty()

        assertFalse(requested.contains("android.permission.USE_EXACT_ALARM"))
        assertFalse(requested.contains("android.permission.BLUETOOTH_SCAN"))
        assertFalse(requested.contains("android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE"))
        assertTrue(requested.contains("android.permission.SCHEDULE_EXACT_ALARM"))
        assertTrue(requested.contains("android.permission.POST_NOTIFICATIONS"))
    }
}
