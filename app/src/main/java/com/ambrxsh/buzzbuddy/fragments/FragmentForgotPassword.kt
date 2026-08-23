package com.ambrxsh.buzzbuddy.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ambrxsh.buzzbuddy.ActivityPreLogin
import com.ambrxsh.buzzbuddy.BuzzBuddyApp
import com.ambrxsh.buzzbuddy.R
import com.ambrxsh.buzzbuddy.clients.AuthClientService
import com.ambrxsh.buzzbuddy.dtos.PasswordResetConfirmDto
import com.ambrxsh.buzzbuddy.dtos.PasswordResetRequestDto
import com.ambrxsh.buzzbuddy.utils.setErrorKeepEndIcon
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class FragmentForgotPassword : Fragment(R.layout.fragment_forgot_password) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailLayout = view.findViewById<TextInputLayout>(R.id.emailLayout)
        val codeLayout = view.findViewById<TextInputLayout>(R.id.codeLayout)
        val newPasswordLayout = view.findViewById<TextInputLayout>(R.id.newPasswordLayout)
        val confirmNewPasswordLayout = view.findViewById<TextInputLayout>(R.id.confirmNewPasswordLayout)
        val inputEmail = view.findViewById<TextInputEditText>(R.id.inputEmail)
        val inputCode = view.findViewById<TextInputEditText>(R.id.inputCode)
        val inputNewPassword = view.findViewById<TextInputEditText>(R.id.inputNewPassword)
        val inputConfirmNewPassword = view.findViewById<TextInputEditText>(R.id.inputConfirmNewPassword)
        val sendCodeButton = view.findViewById<MaterialButton>(R.id.sendCodeButton)
        val resetButton = view.findViewById<MaterialButton>(R.id.resetButton)

        val app = requireActivity().application as? BuzzBuddyApp
        if (app == null) {
            Toast.makeText(requireContext(), R.string.error_app_not_initialized, Toast.LENGTH_SHORT).show()
            return
        }
        val authService = app.retrofit.create(AuthClientService::class.java)

        sendCodeButton.setOnClickListener {
            emailLayout.setErrorKeepEndIcon(null)
            val email = inputEmail.text?.toString()?.trim().orEmpty()
            if (email.isEmpty()) {
                emailLayout.setErrorKeepEndIcon(getString(R.string.error_fill_all_fields))
                return@setOnClickListener
            }
            sendCodeButton.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        authService.requestPasswordReset(PasswordResetRequestDto(email))
                    }
                    Toast.makeText(requireContext(), R.string.reset_code_sent, Toast.LENGTH_LONG).show()
                } catch (e: IOException) {
                    Log.w(TAG, "Reset request network error", e)
                    Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Reset request unexpected error", e)
                    Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show()
                } finally {
                    if (isAdded) sendCodeButton.isEnabled = true
                }
            }
        }

        resetButton.setOnClickListener {
            codeLayout.setErrorKeepEndIcon(null)
            newPasswordLayout.setErrorKeepEndIcon(null)
            confirmNewPasswordLayout.setErrorKeepEndIcon(null)
            val email = inputEmail.text?.toString()?.trim().orEmpty()
            val code = inputCode.text?.toString()?.trim().orEmpty()
            val newPassword = inputNewPassword.text?.toString().orEmpty()
            val confirmPassword = inputConfirmNewPassword.text?.toString().orEmpty()
            var invalid = false
            if (email.isEmpty()) {
                emailLayout.setErrorKeepEndIcon(getString(R.string.error_fill_all_fields))
                invalid = true
            }
            if (code.isEmpty()) {
                codeLayout.setErrorKeepEndIcon(getString(R.string.error_fill_all_fields))
                invalid = true
            }
            if (newPassword.isEmpty()) {
                newPasswordLayout.setErrorKeepEndIcon(getString(R.string.error_fill_all_fields))
                invalid = true
            } else if (newPassword.length < 6) {
                newPasswordLayout.setErrorKeepEndIcon(getString(R.string.error_password_too_short))
                invalid = true
            }
            if (confirmPassword.isEmpty()) {
                confirmNewPasswordLayout.setErrorKeepEndIcon(getString(R.string.error_fill_all_fields))
                invalid = true
            } else if (newPassword.isNotEmpty() && newPassword != confirmPassword) {
                confirmNewPasswordLayout.setErrorKeepEndIcon(getString(R.string.error_passwords_mismatch))
                invalid = true
            }
            if (invalid) return@setOnClickListener

            resetButton.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        authService.confirmPasswordReset(
                            PasswordResetConfirmDto(
                                email = email,
                                code = code,
                                newPassword = newPassword
                            )
                        )
                    }
                    Toast.makeText(requireContext(), R.string.password_reset_success, Toast.LENGTH_LONG).show()
                    (activity as? ActivityPreLogin)?.showLogin()
                } catch (e: HttpException) {
                    Log.w(TAG, "Reset confirm HTTP ${e.code()}", e)
                    codeLayout.setErrorKeepEndIcon(getString(R.string.error_generic) + " (${e.code()})")
                } catch (e: IOException) {
                    Log.w(TAG, "Reset confirm network error", e)
                    Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Reset confirm unexpected error", e)
                    Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show()
                } finally {
                    if (isAdded) resetButton.isEnabled = true
                }
            }
        }
    }

    companion object {
        private const val TAG = "FragmentForgotPassword"
    }
}
