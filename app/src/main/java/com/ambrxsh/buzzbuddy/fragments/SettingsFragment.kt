package com.ambrxsh.buzzbuddy.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ambrxsh.buzzbuddy.ActivityPreLogin
import com.ambrxsh.buzzbuddy.BuzzBuddyApp
import com.ambrxsh.buzzbuddy.R
import com.ambrxsh.buzzbuddy.clients.AuthClientService
import com.ambrxsh.buzzbuddy.dtos.ChangePasswordRequestDto
import com.ambrxsh.buzzbuddy.dtos.LoginRequestDto
import com.ambrxsh.buzzbuddy.dtos.LogoutRequestDto
import com.ambrxsh.buzzbuddy.model.SettingsData
import com.ambrxsh.buzzbuddy.utils.SessionStore
import com.ambrxsh.buzzbuddy.utils.SettingsManager
import com.ambrxsh.buzzbuddy.utils.setTwoDigitRange
import com.ambrxsh.buzzbuddy.utils.apiErrorMessage
import com.ambrxsh.buzzbuddy.utils.setErrorKeepEndIcon
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class SettingsFragment : Fragment() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var settings: SettingsData

    private lateinit var seekBarVolume: SeekBar
    private lateinit var switchGradualVolume: Switch
    private lateinit var switchVibrate: Switch
    private lateinit var switchAutoDismiss: Switch
    private lateinit var tvSnoozeDuration: TextView
    private lateinit var btnEditSnooze: ImageView
    private lateinit var tvAlarmSound: TextView
    private lateinit var btnChangeSound: ImageView
    private lateinit var layoutSnooze: LinearLayout
    private lateinit var layoutAlarmSound: LinearLayout
    private lateinit var layoutGradualVolume: LinearLayout
    private lateinit var layoutVibrate: LinearLayout
    private lateinit var layoutAutoDismiss: LinearLayout

    override fun onCreateView(
        inflater: android.view.LayoutInflater, container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Status bar
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.app_theme)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requireActivity().window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // Toolbar
        val toolbar = view.findViewById<MaterialToolbar>(R.id.settings_toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener {
            if (!parentFragmentManager.popBackStackImmediate()) {
                requireActivity().finish()
            }
        }

        // Settings manager
        settingsManager = SettingsManager(requireContext())
        settings = settingsManager.loadSettings()

        // Bind views
        seekBarVolume = view.findViewById(R.id.seekBarVolume)
        switchGradualVolume = view.findViewById(R.id.switchGradualVolume)
        switchVibrate = view.findViewById(R.id.switchVibrate)
        switchAutoDismiss = view.findViewById(R.id.switchAutoDismiss)
        tvSnoozeDuration = view.findViewById(R.id.tvSnoozeDuration)
        btnEditSnooze = view.findViewById(R.id.btnEditSnooze)
        tvAlarmSound = view.findViewById(R.id.tvAlarmSound)
        btnChangeSound = view.findViewById(R.id.btnChangeSound)

        layoutSnooze = view.findViewById(R.id.layoutSnooze)
        layoutAlarmSound = view.findViewById(R.id.layoutAlarmSound)
        layoutGradualVolume = view.findViewById(R.id.layoutGradualVolume)
        layoutVibrate = view.findViewById(R.id.layoutVibrate)
        layoutAutoDismiss = view.findViewById(R.id.layoutAutoDismiss)

        // Load initial values
        seekBarVolume.progress = settings.volume
        switchGradualVolume.isChecked = settings.gradualVolume
        switchVibrate.isChecked = settings.vibrate
        switchAutoDismiss.isChecked = settings.autoDismiss
        tvSnoozeDuration.text = getString(R.string.snooze_duration_minutes, settings.snoozeDuration)
        tvAlarmSound.text = displayNameForSound(settings.alarmSound)

        // SeekBar listener
        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                settings.volume = progress
                settingsManager.saveSettings(settings)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Switch toggle rows
        layoutGradualVolume.setOnClickListener {
            switchGradualVolume.isChecked = !switchGradualVolume.isChecked
            settings.gradualVolume = switchGradualVolume.isChecked
            settingsManager.saveSettings(settings)
        }

        layoutVibrate.setOnClickListener {
            switchVibrate.isChecked = !switchVibrate.isChecked
            settings.vibrate = switchVibrate.isChecked
            settingsManager.saveSettings(settings)
        }

        layoutAutoDismiss.setOnClickListener {
            switchAutoDismiss.isChecked = !switchAutoDismiss.isChecked
            settings.autoDismiss = switchAutoDismiss.isChecked
            settingsManager.saveSettings(settings)
        }

        // Snooze picker
        val snoozeClickListener = View.OnClickListener {
            val numberPicker = NumberPicker(requireContext()).apply {
                setTextColor("#212121".toColorInt())
                setTwoDigitRange(1, 60)
                value = settings.snoozeDuration.coerceIn(1, 60)
            }

            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(30, 30, 30, 30)
                addView(numberPicker)
            }

            val dialog = AlertDialog.Builder(requireContext(), R.style.Snooze_dialog_theme)
                .setTitle(R.string.snooze_duration_title)
                .setView(layout)
                .setPositiveButton(R.string.ok) { d, _ ->
                    settings.snoozeDuration = numberPicker.value
                    tvSnoozeDuration.text = getString(R.string.snooze_duration_minutes, numberPicker.value)
                    settingsManager.saveSettings(settings)
                    d.dismiss()
                }
                .setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }
                .create()

            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor("#e14f62".toColorInt())
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor("#696969".toColorInt())
        }

        layoutSnooze.setOnClickListener(snoozeClickListener)
        btnEditSnooze.setOnClickListener(snoozeClickListener)

        // Alarm sound toggle on row or button
        val alarmSoundClick = View.OnClickListener {
            val sunrise = getString(R.string.alarm_sound_sunrise)
            val beep = getString(R.string.alarm_sound_beep)
            val newSound = if (settings.alarmSound == beep) sunrise else beep
            settings.alarmSound = newSound
            tvAlarmSound.text = displayNameForSound(newSound)
            settingsManager.saveSettings(settings)
        }

        layoutAlarmSound.setOnClickListener(alarmSoundClick)
        btnChangeSound.setOnClickListener(alarmSoundClick)

        bindAccountSection(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { refreshAccountUi(it) }
    }

    private fun bindAccountSection(view: View) {
        refreshAccountUi(view)

        view.findViewById<View>(R.id.layoutLogin).setOnClickListener {
            startActivity(
                Intent(requireContext(), ActivityPreLogin::class.java).apply {
                    putExtra(ActivityPreLogin.EXTRA_FORCE_AUTH, true)
                }
            )
        }
        view.findViewById<View>(R.id.layoutRegister).setOnClickListener {
            startActivity(
                Intent(requireContext(), ActivityPreLogin::class.java).apply {
                    putExtra(ActivityPreLogin.EXTRA_FORCE_AUTH, true)
                    putExtra(ActivityPreLogin.EXTRA_START_REGISTER, true)
                }
            )
        }
        view.findViewById<View>(R.id.layoutChangePassword).setOnClickListener {
            showChangePasswordDialog()
        }
        view.findViewById<View>(R.id.layoutLogout).setOnClickListener {
            AlertDialog.Builder(requireContext(), R.style.Snooze_dialog_theme)
                .setTitle(R.string.log_out)
                .setMessage(R.string.logout_confirm)
                .setPositiveButton(R.string.log_out) { _, _ -> performLogout() }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        view.findViewById<View>(R.id.layoutDeleteAccount).setOnClickListener {
            AlertDialog.Builder(requireContext(), R.style.Snooze_dialog_theme)
                .setTitle(R.string.delete_account)
                .setMessage(R.string.delete_account_confirm)
                .setPositiveButton(android.R.string.ok) { _, _ -> deleteAccount() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        val session = SessionStore(requireContext())
        if (!session.isLoggedIn()) return
        val service = authService() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = withContext(Dispatchers.IO) { service.me() }
                session.saveProfile(user.name, user.email)
                view.let { refreshAccountUi(it) }
            } catch (e: Exception) {
                Log.w(TAG, "load profile failed", e)
            }
        }
    }

    private fun refreshAccountUi(view: View) {
        val session = SessionStore(requireContext())
        val loggedIn = session.isLoggedIn()
        val nameView = view.findViewById<TextView>(R.id.tvAccountName)
        val emailView = view.findViewById<TextView>(R.id.tvAccountEmail)
        if (loggedIn) {
            val name = session.getName().orEmpty()
            val email = session.getEmail().orEmpty()
            nameView.text = name.ifBlank { getString(R.string.account_name_placeholder) }
            emailView.text = email.ifBlank { getString(R.string.account_email_placeholder) }
        }
        view.findViewById<View>(R.id.layoutAccountInfo).visibility = if (loggedIn) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.layoutChangePassword).visibility = if (loggedIn) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.layoutLogout).visibility = if (loggedIn) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.layoutDeleteAccount).visibility = if (loggedIn) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.tvGuestAccountHint).visibility = if (loggedIn) View.GONE else View.VISIBLE
        view.findViewById<View>(R.id.layoutLogin).visibility = if (loggedIn) View.GONE else View.VISIBLE
        view.findViewById<View>(R.id.layoutRegister).visibility = if (loggedIn) View.GONE else View.VISIBLE
    }

    private fun authService(): AuthClientService? {
        val app = requireActivity().application as? BuzzBuddyApp
        if (app == null) {
            Toast.makeText(requireContext(), R.string.error_app_not_initialized, Toast.LENGTH_SHORT).show()
            return null
        }
        return app.retrofit.create(AuthClientService::class.java)
    }

    private fun showChangePasswordDialog() {
        val content = layoutInflater.inflate(R.layout.dialog_change_password, null, false)
        val currentLayout = content.findViewById<TextInputLayout>(R.id.currentPasswordLayout)
        val newLayout = content.findViewById<TextInputLayout>(R.id.newPasswordLayout)
        val confirmLayout = content.findViewById<TextInputLayout>(R.id.confirmNewPasswordLayout)
        val currentInput = content.findViewById<TextInputEditText>(R.id.inputCurrentPassword)
        val newInput = content.findViewById<TextInputEditText>(R.id.inputNewPassword)
        val confirmInput = content.findViewById<TextInputEditText>(R.id.inputConfirmNewPassword)

        val dialog = AlertDialog.Builder(requireContext(), R.style.Snooze_dialog_theme)
            .setTitle(R.string.change_password)
            .setView(content)
            .setPositiveButton(R.string.action_save_password, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            saveButton.setTextColor("#e14f62".toColorInt())
            cancelButton.setTextColor("#696969".toColorInt())
            saveButton.setOnClickListener {
                currentLayout.setErrorKeepEndIcon(null)
                newLayout.setErrorKeepEndIcon(null)
                confirmLayout.setErrorKeepEndIcon(null)

                val current = currentInput.text?.toString().orEmpty()
                val next = newInput.text?.toString().orEmpty()
                val confirm = confirmInput.text?.toString().orEmpty()
                var invalid = false
                if (current.isEmpty()) {
                    currentLayout.setErrorKeepEndIcon(getString(R.string.error_fill_all_fields))
                    invalid = true
                }
                if (next.isEmpty()) {
                    newLayout.setErrorKeepEndIcon(getString(R.string.error_fill_all_fields))
                    invalid = true
                } else if (next.length < 6) {
                    newLayout.setErrorKeepEndIcon(getString(R.string.error_password_too_short))
                    invalid = true
                }
                if (confirm.isEmpty()) {
                    confirmLayout.setErrorKeepEndIcon(getString(R.string.error_fill_all_fields))
                    invalid = true
                } else if (next.isNotEmpty() && next != confirm) {
                    confirmLayout.setErrorKeepEndIcon(getString(R.string.error_passwords_mismatch))
                    invalid = true
                }
                if (invalid) return@setOnClickListener

                val service = authService() ?: return@setOnClickListener
                val session = SessionStore(requireContext())
                val email = session.getEmail().orEmpty()
                saveButton.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            service.changePassword(ChangePasswordRequestDto(current, next))
                            if (email.isNotBlank()) {
                                val tokens = service.login(LoginRequestDto(email, next))
                                val access = tokens.accessToken.ifBlank { tokens.token.orEmpty() }
                                session.saveSession(access, tokens.refreshToken, email)
                            }
                        }
                        Toast.makeText(requireContext(), R.string.password_changed, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } catch (e: HttpException) {
                        Log.w(TAG, "change password HTTP ${e.code()}", e)
                        val msg = e.apiErrorMessage(getString(R.string.error_generic))
                        if (e.code() == 400 || e.code() == 401) {
                            currentLayout.setErrorKeepEndIcon(getString(R.string.error_invalid_credentials))
                        } else {
                            newLayout.setErrorKeepEndIcon(msg)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "change password failed", e)
                        Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show()
                    } finally {
                        if (dialog.isShowing) saveButton.isEnabled = true
                    }
                }
            }
        }
        dialog.show()
    }

    private fun performLogout() {
        val service = authService()
        val appContext = requireContext().applicationContext
        val session = SessionStore(appContext)
        val refresh = session.getRefreshToken()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (service != null) {
                    withContext(Dispatchers.IO) {
                        service.logout(LogoutRequestDto(refresh))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "logout API failed", e)
            }
            session.clear()
            session.markAuthGateCompleted()
            Toast.makeText(appContext, R.string.logged_out, Toast.LENGTH_SHORT).show()
            view?.let { refreshAccountUi(it) }
        }
    }

    private fun deleteAccount() {
        val service = authService() ?: return
        val appContext = requireContext().applicationContext
        val session = SessionStore(appContext)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { service.deleteAccount() }
                session.clear()
                session.markAuthGateCompleted()
                Toast.makeText(appContext, R.string.account_deleted, Toast.LENGTH_SHORT).show()
                view?.let { refreshAccountUi(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "delete account failed", e)
                Toast.makeText(appContext, R.string.error_generic, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val TAG = "SettingsFragment"
    }

    private fun displayNameForSound(stored: String): String {
        val beep = getString(R.string.alarm_sound_beep)
        return if (stored == beep || stored.equals("Beep", ignoreCase = true)) {
            getString(R.string.alarm_sound_beep)
        } else {
            getString(R.string.alarm_sound_sunrise)
        }
    }
}
