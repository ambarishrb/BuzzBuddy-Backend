package com.ambrxsh.buzzbuddy.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ambrxsh.buzzbuddy.AlarmPlayer
import com.ambrxsh.buzzbuddy.AlarmReceiver
import com.ambrxsh.buzzbuddy.R
import com.ambrxsh.buzzbuddy.scheduler.AlarmRescheduler
import com.ambrxsh.buzzbuddy.scheduler.BuzzBuddyAlarmScheduler
import com.ambrxsh.buzzbuddy.utils.SettingsManager
import com.ambrxsh.buzzbuddy.utils.SnoozeManager
import com.ambrxsh.buzzbuddy.viewmodel.SmartAlarmViewModel

class ActivityAlarmFragment : Fragment() {

    private lateinit var alarmScheduler: BuzzBuddyAlarmScheduler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_activity_alarm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        alarmScheduler = BuzzBuddyAlarmScheduler(requireContext())

        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.activityalarm_bg)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        val alarmLabelTextView = view.findViewById<TextView>(R.id.tvAlarmLabel)
        val alarmTimeTextView = view.findViewById<TextView>(R.id.tvAlarmTime)
        alarmTimeTextView.text = AlarmPlayer.getAlarmTime(requireContext())

        val alarmId = arguments?.getInt(BuzzBuddyAlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1
        if (alarmId != -1 && AlarmRescheduler.isUserUnlocked(requireContext())) {
            val smartAlarmViewModel = ViewModelProvider(this)[SmartAlarmViewModel::class.java]
            smartAlarmViewModel.getAlarmById(alarmId).observe(viewLifecycleOwner) { alarm ->
                alarmLabelTextView.text = alarm?.alarmTitle?.takeIf { it.isNotBlank() }
            }
        } else {
            alarmLabelTextView.text = null
        }

        view.findViewById<Button>(R.id.dismiss).setOnClickListener {
            stopAlarm(alarmId)
            Toast.makeText(requireContext(), R.string.alarm_dismissed, Toast.LENGTH_SHORT).show()
            activity?.finish()
        }

        view.findViewById<Button>(R.id.btnSnooze).setOnClickListener {
            val settingsManager = SettingsManager(requireContext())
            val snoozeMinutes = settingsManager.loadSettings().snoozeDuration

            stopAlarm(alarmId)

            val triggerAt = alarmScheduler.scheduleSnooze(alarmId, snoozeMinutes)
            if (triggerAt != null) {
                SnoozeManager.get(requireContext()).markSnoozed(alarmId, triggerAt)
            }

            Toast.makeText(
                requireContext(),
                getString(R.string.alarm_snoozed, snoozeMinutes),
                Toast.LENGTH_SHORT
            ).show()
            activity?.finish()
        }
    }

    private fun stopAlarm(alarmId: Int) {
        val stopIntent = Intent(requireContext(), AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_STOP_ALARM
            putExtra(BuzzBuddyAlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        requireContext().sendBroadcast(stopIntent)
    }
}
