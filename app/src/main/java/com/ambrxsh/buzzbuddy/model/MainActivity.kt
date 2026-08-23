package com.ambrxsh.buzzbuddy.model

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.ambrxsh.buzzbuddy.R
import com.ambrxsh.buzzbuddy.databinding.ActivityMainBinding
import com.ambrxsh.buzzbuddy.fragments.ActivityAlarmFragment
import com.ambrxsh.buzzbuddy.fragments.SetAlarmPage
import com.ambrxsh.buzzbuddy.scheduler.AlarmRescheduler
import com.ambrxsh.buzzbuddy.utils.AlarmPermissionHelper
import com.ambrxsh.buzzbuddy.viewmodel.SmartAlarmViewModel

class MainActivity : AppCompatActivity() {

    lateinit var smartAlarmViewModel: SmartAlarmViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        smartAlarmViewModel = ViewModelProvider(this)[SmartAlarmViewModel::class.java]
        AlarmPermissionHelper.requestStartupPermissions(this)

        enableEdgeToEdge()

        if (savedInstanceState == null) {
            if (intent.getBooleanExtra("openAlarmFragment", false)) {
                openAlarmFragment()
            } else {
                openSetAlarmPage()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AlarmRescheduler.restoreAsync(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("openAlarmFragment", false)) {
            openAlarmFragment()
        } else {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    companion object {
        fun startAtHome(context: Context) {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
        }
    }

    private fun openSetAlarmPage() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SetAlarmPage())
            .commit()
    }

    private fun openAlarmFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ActivityAlarmFragment())
            .addToBackStack("Alarm")
            .commit()
    }
}
