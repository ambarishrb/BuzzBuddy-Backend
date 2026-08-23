package com.ambrxsh.buzzbuddy.fragments

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ambrxsh.buzzbuddy.R
import com.ambrxsh.buzzbuddy.adapter.AlarmAdapter
import com.ambrxsh.buzzbuddy.databinding.FragmentSetAlarmBinding
import com.ambrxsh.buzzbuddy.model.SmartAlarm
import com.ambrxsh.buzzbuddy.scheduler.AlarmScheduleMath
import com.ambrxsh.buzzbuddy.scheduler.BuzzBuddyAlarmScheduler
import com.ambrxsh.buzzbuddy.utils.AlarmPermissionHelper
import com.ambrxsh.buzzbuddy.utils.AlarmTimeFormat
import com.ambrxsh.buzzbuddy.utils.SnoozeManager
import com.ambrxsh.buzzbuddy.utils.setPickerTextColor
import com.ambrxsh.buzzbuddy.utils.setTwoDigitRange
import com.ambrxsh.buzzbuddy.viewmodel.SmartAlarmViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Calendar

class SetAlarmPage : Fragment() {

    private var _binding: FragmentSetAlarmBinding? = null
    private val binding get() = _binding!!

    private lateinit var smartAlarmViewModel: SmartAlarmViewModel
    private lateinit var alarmAdapter: AlarmAdapter
    private lateinit var alarmStatus: TextView
    private lateinit var alarmScheduler: BuzzBuddyAlarmScheduler
    private var latestAlarms: List<SmartAlarm> = emptyList()
    private var latestSnoozes: Map<Int, Long> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            }
        )

        alarmScheduler = BuzzBuddyAlarmScheduler(requireContext())
        val snoozeManager = SnoozeManager.get(requireContext())

        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.app_theme)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        smartAlarmViewModel = ViewModelProvider(requireActivity())[SmartAlarmViewModel::class.java]

        alarmAdapter = AlarmAdapter(object : AlarmAdapter.Listener {
            override fun onAlarmToggled(alarm: SmartAlarm, isEnabled: Boolean) {
                alarm.isEnabled = isEnabled
                smartAlarmViewModel.update(alarm)

                if (isEnabled) {
                    scheduleOrPrompt(alarm.alarmId, alarm.alarmTime_hour, alarm.alarmTime_minute)
                } else {
                    alarmScheduler.cancel(alarm.alarmId)
                    snoozeManager.clearSnooze(alarm.alarmId)
                }
            }

            override fun onCancelSnooze(alarm: SmartAlarm) {
                snoozeManager.cancelSnooze(alarm.alarmId)
                Toast.makeText(requireContext(), R.string.snooze_cancelled, Toast.LENGTH_SHORT).show()
            }
        })

        binding.recyclerViewAlarms.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewAlarms.adapter = alarmAdapter

        alarmStatus = binding.tvNextAlarm
        binding.permissionBanner.setOnClickListener {
            AlarmPermissionHelper.repairNextGap(requireActivity())
        }

        smartAlarmViewModel.getAllAlarms().observe(viewLifecycleOwner) { alarms ->
            latestAlarms = alarms
            alarmAdapter.alarmList = alarms
            alarmAdapter.notifyDataSetChanged()
            updateNextAlarmStatus()
        }

        snoozeManager.activeSnoozes.observe(viewLifecycleOwner) { snoozes ->
            latestSnoozes = snoozes
            alarmAdapter.snoozeUntilById = snoozes
            alarmAdapter.notifyDataSetChanged()
            updateNextAlarmStatus()
        }

        binding.settingsIcon.setOnClickListener {
            val settingsFragment = SettingsFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, settingsFragment)
                .addToBackStack("settings")
                .commit()
        }

        binding.addAlarmButton.setOnClickListener { showCustomDialog() }

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val alarmToDelete = alarmAdapter.returnItemGivenPosition(position)

                alarmScheduler.cancel(alarmToDelete.alarmId)
                snoozeManager.clearSnooze(alarmToDelete.alarmId)
                smartAlarmViewModel.delete(alarmToDelete)

                val currentList = alarmAdapter.alarmList.toMutableList()
                currentList.removeAt(position)
                alarmAdapter.alarmList = currentList
                alarmAdapter.notifyItemRemoved(position)

                Snackbar.make(binding.root, R.string.alarm_deleted, Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.addAlarmButton)
                    .setAction(R.string.undo) {
                        lifecycleScope.launch {
                            smartAlarmViewModel.restore(alarmToDelete)
                            if (alarmToDelete.isEnabled) {
                                scheduleOrPrompt(
                                    alarmToDelete.alarmId,
                                    alarmToDelete.alarmTime_hour,
                                    alarmToDelete.alarmTime_minute
                                )
                            }

                            val updatedList = alarmAdapter.alarmList.toMutableList()
                            val insertAt = position.coerceIn(0, updatedList.size)
                            updatedList.add(insertAt, alarmToDelete)
                            alarmAdapter.alarmList = updatedList
                            alarmAdapter.notifyItemInserted(insertAt)
                        }
                    }.show()
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerViewAlarms)
        refreshPermissionBanner()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionBanner()
    }

    @SuppressLint("DefaultLocale")
    private fun showCustomDialog() {
        val builder = android.app.AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.custom_dialog_layout, null)
        builder.setView(dialogView)
        val dialog = builder.create()

        val hourPicker = dialogView.findViewById<NumberPicker>(R.id.hour_picker).apply {
            setTwoDigitRange(0, 23)
            setPickerTextColor("#212121".toColorInt())
        }

        val minutePicker = dialogView.findViewById<NumberPicker>(R.id.minute_picker).apply {
            setTwoDigitRange(0, 59)
            setPickerTextColor("#212121".toColorInt())
        }

        val alarmTitleEt = dialogView.findViewById<EditText>(R.id.alarm_title)

        dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.set_button).setOnClickListener {
            val selectedHour = hourPicker.value
            val selectedMinute = minutePicker.value

            lifecycleScope.launch {
                val existingAlarm = smartAlarmViewModel.getAlarmByTime(selectedHour, selectedMinute)
                if (existingAlarm != null) {
                    Toast.makeText(requireContext(), R.string.alarm_already_set, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@launch
                }

                val newAlarm = SmartAlarm(
                    alarmTitle = alarmTitleEt.text.toString(),
                    alarmTime_hour = selectedHour,
                    alarmTime_minute = selectedMinute,
                    isEnabled = true,
                )

                val generatedId = smartAlarmViewModel.insertAndReturnId(newAlarm).toInt()
                newAlarm.alarmId = generatedId
                smartAlarmViewModel.updateAndWait(newAlarm)

                scheduleOrPrompt(newAlarm.alarmId, selectedHour, selectedMinute)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateNextAlarmStatus() {
        val enabledAlarms = latestAlarms.filter { it.isEnabled }
        if (enabledAlarms.isEmpty()) {
            alarmStatus.text = getString(R.string.no_upcoming_alarms)
            alarmStatus.setTextColor("#8E8E93".toColorInt())
            return
        }

        val now = System.currentTimeMillis()
        val nextMillis = enabledAlarms.minOf { alarm ->
            AlarmScheduleMath.nextDueMillis(
                alarm.alarmTime_hour,
                alarm.alarmTime_minute,
                latestSnoozes[alarm.alarmId],
                now
            )
        }
        val calendar = Calendar.getInstance().apply { timeInMillis = nextMillis }
        alarmStatus.text = getString(
            R.string.next_alarm_at,
            AlarmTimeFormat.format12Hour(
                requireContext(),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)
            )
        )
        alarmStatus.setTextColor("#8E8E93".toColorInt())
    }

    private fun refreshPermissionBanner() {
        val show = AlarmPermissionHelper.hasReliabilityGap(requireContext())
        binding.permissionBanner.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun scheduleOrPrompt(alarmId: Int, hour: Int, minute: Int) {
        if (!alarmScheduler.schedule(alarmId, hour, minute)) {
            AlarmPermissionHelper.requestExactAlarmPermission(requireActivity())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
