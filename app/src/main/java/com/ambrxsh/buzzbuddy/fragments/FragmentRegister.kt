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
import com.ambrxsh.buzzbuddy.dtos.RegisterRequestDto
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

class FragmentRegister : Fragment(R.layout.fragment_register) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nameLayout = view.findViewById<TextInputLayout>(R.id.nameLayout)
        val emailLayout = view.findViewById<TextInputLayout>(R.id.emailLayout)
        val passwordLayout = view.findViewById<TextInputLayout>(R.id.passwordLayout)
        val confirmLayout = view.findViewById<TextInputLayout>(R.id.confirmPasswordLayout)
        val inputName = view.findViewById<TextInputEditText>(R.id.inputName)
        val inputEmail = view.findViewById<TextInputEditText>(R.id.inputEmail)
        val inputPassword = view.findViewById<TextInputEditText>(R.id.inputPassword)
        val inputConfirmPassword = view.findViewById<TextInputEditText>(R.id.inputConfirmPassword)
        val registerButton = view.findViewById<MaterialButton>(R.id.registerButton)

        view.findViewById<View>(R.id.loginLink).setOnClickListener {
            (activity as? ActivityPreLogin)?.showLogin()
        }
        val continueButton = view.findViewById<View>(R.id.continueWithoutLogin)
        val forceAuth = (activity as? ActivityPreLogin)?.isForceAuth() == true
        continueButton.visibility = if (forceAuth) View.GONE else View.VISIBLE
        continueButton.setOnClickListener {
            (activity as? ActivityPreLogin)?.continueWithoutAccount()
        }

        registerButton.setOnClickListener {
            nameLayout.setErrorKeepEndIcon(null)
            emailLayout.setErrorKeepEndIcon(null)
            passwordLayout.setErrorKeepEndIcon(null)
            confirmLayout.setErrorKeepEndIcon(null)

            val name = inputName.text?.toString()?.trim().orEmpty()
            val email = inputEmail.text?.toString()?.trim().orEmpty()
            val password = inputPassword.text?.toString().orEmpty()
            val confirmPassword = inputConfirmPassword.text?.toString().orEmpty()

            var invalid = false
            if (name.isEmpty()) {
                nameLayout.setErrorKeepEndIcon(getString(R.string.error_fill_all_fields))
                invalid = true
            }
            if (email.isEmpty()) {
                emailLayout.setErrorKeepEndIcon(getString(R.string.error_fill_all_fields))
                invalid = true
            } else if (!email.contains("@") || !email.contains(".")) {
                emailLayout.setErrorKeepEndIcon(getString(R.string.error_invalid_email))
                invalid = true
            }
            if (password.isEmpty()) {
                passwordLayout.setErrorKeepEndIcon(getString(R.string.error_fill_all_fields))
                invalid = true
            } else if (password.length < 6) {
                passwordLayout.setErrorKeepEndIcon(getString(R.string.error_password_too_short))
                invalid = true
            }
            if (confirmPassword.isEmpty()) {
                confirmLayout.setErrorKeepEndIcon(getString(R.string.error_fill_all_fields))
                invalid = true
            }
            if (password.isNotEmpty() && password != confirmPassword) {
                confirmLayout.setErrorKeepEndIcon(getString(R.string.error_passwords_mismatch))
                invalid = true
            }
            if (invalid) return@setOnClickListener

            val app = requireActivity().application as? BuzzBuddyApp
            if (app == null) {
                Toast.makeText(requireContext(), R.string.error_app_not_initialized, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val authService = app.retrofit.create(AuthClientService::class.java)

            registerButton.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        authService.register(
                            RegisterRequestDto(
                                name = name,
                                email = email,
                                password = password,
                                confirmPassword = confirmPassword
                            )
                        )
                    }
                    Toast.makeText(requireContext(), R.string.register_success, Toast.LENGTH_LONG).show()
                    (activity as? ActivityPreLogin)?.showLogin()
                } catch (e: HttpException) {
                    Log.w(TAG, "Register HTTP ${e.code()}", e)
                    val serverMsg = e.apiErrorMessage("")
                    when {
                        e.code() == 409 || serverMsg.contains("already", ignoreCase = true) ->
                            emailLayout.setErrorKeepEndIcon(getString(R.string.error_email_registered))
                        serverMsg.contains("password", ignoreCase = true) ->
                            passwordLayout.setErrorKeepEndIcon(
                                serverMsg.ifBlank { getString(R.string.error_password_too_short) }
                            )
                        serverMsg.contains("email", ignoreCase = true) ->
                            emailLayout.setErrorKeepEndIcon(
                                serverMsg.ifBlank { getString(R.string.error_invalid_email) }
                            )
                        else ->
                            Toast.makeText(
                                requireContext(),
                                serverMsg.ifBlank { getString(R.string.error_invalid_registration) },
                                Toast.LENGTH_LONG
                            ).show()
                    }
                } catch (e: IOException) {
                    Log.w(TAG, "Register network error", e)
                    Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Register unexpected error", e)
                    Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show()
                } finally {
                    if (isAdded) registerButton.isEnabled = true
                }
            }
        }
    }

    companion object {
        private const val TAG = "FragmentRegister"
    }
}
