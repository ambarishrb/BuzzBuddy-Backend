package com.ambrxsh.buzzbuddy.phase3

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ambrxsh.buzzbuddy.BootReceiver
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase3BootReceiverRegisteredTest {

    @Test
    fun bootReceiverIsEnabledInThePackage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val component = ComponentName(context, BootReceiver::class.java)
        val state = context.packageManager.getComponentEnabledSetting(component)
        assertTrue(
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        )
        val info = context.packageManager.getReceiverInfo(component, 0)
        assertTrue(info.enabled)
    }
}
