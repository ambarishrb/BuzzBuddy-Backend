package com.ambrxsh.buzzbuddy.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ambrxsh.buzzbuddy.ActivityPreLogin
import com.ambrxsh.buzzbuddy.BuzzBuddyApp
import com.ambrxsh.buzzbuddy.model.MainActivity
import com.ambrxsh.buzzbuddy.R
import com.ambrxsh.buzzbuddy.clients.AuthClientService
import com.ambrxsh.buzzbuddy.dtos.LoginRequestDto
import com.ambrxsh.buzzbuddy.sync.AlarmSync
import com.ambrxsh.buzzbuddy.utils.SessionStore
import com.ambrxsh.buzzbuddy.utils.apiErrorMessage
import com.ambrxsh.buzzbuddy.utils.setErrorKeepEndIcon
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class FragmentLogin : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailLayout = view.findViewById<TextInputLayout>(R.id.emailLayout)
        val passwordLayout = view.findViewById<TextInputLayout>(R.id.passwordLayout)
        val email = view.findViewById<TextInputEditText>(R.id.inputEmail)
        val password = view.findViewById<TextInputEditText>(R.id.inputPassword)
        val loginButton = view.findViewById<MaterialButton>(R.id.loginButton)

        view.findViewById<View>(R.id.forgotPassword).setOnClickListener {
            (activity as? ActivityPreLogin)?.showForgotPassword()
        }
        view.findViewById<View>(R.id.registerLink).setOnClickListener {
            (activity as? ActivityPreLogin)?.showRegister()
        }
        val continueButton = view.findViewById<View>(R.id.continueWithoutLogin)
        val forceAuth = (activity as? ActivityPreLogin)?.isForceAuth() == true
        continueButton.visibility = if (forceAuth) View.GONE else View.VISIBLE
        continueButton.setOnClickListener {
            (activity as? ActivityPreLogin)?.continueWithoutAccount()
        }

        loginButton.setOnClickListener {
            emailLayout.setErrorKeepEndIcon(null)
            passwordLayout.setErrorKeepEndIcon(null)

            val emailText = email.text?.toString()?.trim().orEmpty()
            val passwordText = password.text?.toString().orEmpty()
            if (emailText.isEmpty() || passwordText.isEmpty()) {
                if (emailText.isEmpty()) emailLayout.setErrorKeepEndIcon(getString(R.string.error_fill_all_fields))
                if (passwordText.isEmpty()) passwordLayout.setErrorKeepEndIcon(getString(R.string.error_fill_all_fields))
                return@setOnClickListener
            }
            if (!emailText.contains("@") || !emailText.contains(".")) {
                emailLayout.setErrorKeepEndIcon(getString(R.string.error_invalid_email))
                return@setOnClickListener
            }

            val app = requireActivity().application as? BuzzBuddyApp
            if (app == null) {
                Toast.makeText(requireContext(), R.string.error_app_not_initialized, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val authService = app.retrofit.create(AuthClientService::class.java)
            val session = SessionStore(requireContext())

            loginButton.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        authService.login(LoginRequestDto(emailText, passwordText))
                    }
                    val access = response.accessToken.ifBlank { response.token.orEmpty() }
                    session.saveSession(access, response.refreshToken, emailText)
                    withContext(Dispatchers.IO) {
                        try {
                            val user = authService.me()
                            session.saveProfile(user.name, user.email.ifBlank { emailText })
                        } catch (e: Exception) {
                            Log.w(TAG, "profile fetch failed", e)
                        }
                        AlarmSync.restoreFromServer(requireContext().applicationContext, app)
                    }
                    Toast.makeText(requireContext(), R.string.login_success, Toast.LENGTH_SHORT).show()
                    MainActivity.startAtHome(requireActivity())
                    requireActivity().finish()
                } catch (e: HttpException) {
                    Log.w(TAG, "Login HTTP ${e.code()}", e)
                    val serverMsg = e.apiErrorMessage("")
                    when {
                        e.code() == 401 && serverMsg.contains("not found", ignoreCase = true) -> {
                            emailLayout.setErrorKeepEndIcon(getString(R.string.error_user_not_found))
                        }
                        e.code() == 401 -> {
                            passwordLayout.setErrorKeepEndIcon(getString(R.string.error_invalid_credentials))
                        }
                        e.code() == 400 && serverMsg.contains("email", ignoreCase = true) -> {
                            emailLayout.setErrorKeepEndIcon(serverMsg.ifBlank { getString(R.string.error_invalid_email) })
                        }
                        else -> {
                            passwordLayout.setErrorKeepEndIcon(
                                serverMsg.ifBlank { getString(R.string.error_generic) + " (${e.code()})" }
                            )
                        }
                    }
                } catch (e: IOException) {
                    Log.w(TAG, "Login network error", e)
                    Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Login unexpected error", e)
                    Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show()
                } finally {
                    if (isAdded) loginButton.isEnabled = true
                }
            }
        }
    }

    companion object {
        private const val TAG = "FragmentLogin"
    }
}
