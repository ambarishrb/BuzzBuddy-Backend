package com.ambrxsh.buzzbuddy

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ambrxsh.buzzbuddy.model.SmartAlarm
import com.ambrxsh.buzzbuddy.scheduler.BuzzBuddyAlarmScheduler
import com.ambrxsh.buzzbuddy.utils.AlarmPermissionHelper
import com.ambrxsh.buzzbuddy.utils.setPickerTextColor
import com.ambrxsh.buzzbuddy.utils.setTwoDigitRange
import com.ambrxsh.buzzbuddy.viewmodel.SmartAlarmViewModel
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class EditAlarmActivity : AppCompatActivity() {

    private lateinit var smartAlarmViewModel: SmartAlarmViewModel
    private var alarmId: Int = -1
    private var alarm: SmartAlarm? = null

    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var alarmTitleText: TextView
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_alarm)

        val toolbar = findViewById<MaterialToolbar>(R.id.update_toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        window.statusBarColor = ContextCompat.getColor(this, R.color.app_theme)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        smartAlarmViewModel = ViewModelProvider(this)[SmartAlarmViewModel::class.java]
        val alarmScheduler = BuzzBuddyAlarmScheduler(this)

        alarmId = intent.getIntExtra("alarmId", -1)
        if (alarmId == -1) {
            finish()
            return
        }

        hourPicker = findViewById(R.id.hour_picker)
        minutePicker = findViewById(R.id.minute_picker)
        alarmTitleText = findViewById(R.id.alarm_title)
        saveButton = findViewById(R.id.save_button)
        cancelButton = findViewById(R.id.delete_button)

        smartAlarmViewModel.getAllAlarms().observe(this) { alarms ->
            val loaded = alarms.find { it.alarmId == alarmId } ?: return@observe
            alarm = loaded

            hourPicker.setTwoDigitRange(0, 23)
            hourPicker.value = loaded.alarmTime_hour
            hourPicker.setPickerTextColor("#212121".toColorInt())

            minutePicker.setTwoDigitRange(0, 59)
            minutePicker.value = loaded.alarmTime_minute
            minutePicker.setPickerTextColor("#212121".toColorInt())

            alarmTitleText.text = loaded.alarmTitle
        }

        saveButton.setOnClickListener {
            val current = alarm ?: return@setOnClickListener
            val hour = hourPicker.value
            val minute = minutePicker.value

            saveButton.isEnabled = false
            lifecycleScope.launch {
                val duplicate = smartAlarmViewModel.getAlarmByTimeExcluding(hour, minute, current.alarmId)
                if (duplicate != null) {
                    Toast.makeText(this@EditAlarmActivity, R.string.alarm_already_set, Toast.LENGTH_SHORT).show()
                    saveButton.isEnabled = true
                    return@launch
                }

                current.alarmTime_hour = hour
                current.alarmTime_minute = minute
                current.alarmTitle = alarmTitleText.text.toString()
                smartAlarmViewModel.updateAndWait(current)
                alarmScheduler.cancel(current.alarmId)
                if (current.isEnabled) {
                    if (!alarmScheduler.schedule(current.alarmId, current.alarmTime_hour, current.alarmTime_minute)) {
                        AlarmPermissionHelper.requestExactAlarmPermission(this@EditAlarmActivity)
                    }
                }
                finish()
            }
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }
}
