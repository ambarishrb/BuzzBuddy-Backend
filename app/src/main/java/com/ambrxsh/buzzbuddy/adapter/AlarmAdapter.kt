package com.ambrxsh.buzzbuddy.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ambrxsh.buzzbuddy.EditAlarmActivity
import com.ambrxsh.buzzbuddy.R
import com.ambrxsh.buzzbuddy.databinding.AlarmItemBinding
import com.ambrxsh.buzzbuddy.model.SmartAlarm
import com.ambrxsh.buzzbuddy.utils.AlarmTimeFormat

class AlarmAdapter(
    private val listener: Listener
) : RecyclerView.Adapter<AlarmAdapter.SmartAlarmViewHolder>() {

    interface Listener {
        fun onAlarmToggled(alarm: SmartAlarm, isEnabled: Boolean)
        fun onCancelSnooze(alarm: SmartAlarm)
    }

    var alarmList: List<SmartAlarm> = ArrayList()
    var snoozeUntilById: Map<Int, Long> = emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmartAlarmViewHolder {
        val binding = AlarmItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SmartAlarmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SmartAlarmViewHolder, position: Int) {
        val smartAlarm = alarmList[position]
        val hour24 = smartAlarm.alarmTime_hour
        val minute = smartAlarm.alarmTime_minute
        val context = holder.binding.root.context
        val alarmTimeString = AlarmTimeFormat.format12HourClock(context, hour24, minute)
        val snoozeUntil = snoozeUntilById[smartAlarm.alarmId]

        with(holder.binding) {
            alarmTime.text = alarmTimeString
            amPmText.text = AlarmTimeFormat.amPm(context, hour24)
            alarmTitle.text = smartAlarm.alarmTitle

            if (snoozeUntil != null && snoozeUntil > System.currentTimeMillis()) {
                snoozeRow.visibility = View.VISIBLE
                tvSnoozeStatus.text = root.context.getString(
                    R.string.snooze_until,
                    formatTriggerTime(root.context, snoozeUntil)
                )
                btnStopSnooze.setOnClickListener { listener.onCancelSnooze(smartAlarm) }
            } else {
                snoozeRow.visibility = View.GONE
                btnStopSnooze.setOnClickListener(null)
            }

            alarmCard.setOnClickListener {
                val intent = Intent(it.context, EditAlarmActivity::class.java)
                intent.putExtra("alarmId", smartAlarm.alarmId)
                it.context.startActivity(intent)
            }

            alarmSwitch.setOnCheckedChangeListener(null)
            alarmSwitch.isChecked = smartAlarm.isEnabled
            alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
                listener.onAlarmToggled(smartAlarm, isChecked)
            }
        }
    }

    fun returnItemGivenPosition(position: Int): SmartAlarm = alarmList.getOrElse(position) {
        error("Invalid alarm list position $position")
    }

    override fun getItemCount(): Int = alarmList.size

    private fun formatTriggerTime(context: android.content.Context, triggerAt: Long): String {
        return AlarmTimeFormat.formatMillis(context, triggerAt)
    }

    class SmartAlarmViewHolder(val binding: AlarmItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}
