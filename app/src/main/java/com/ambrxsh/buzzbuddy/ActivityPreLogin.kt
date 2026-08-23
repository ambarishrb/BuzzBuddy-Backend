package com.ambrxsh.buzzbuddy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ambrxsh.buzzbuddy.fragments.FragmentForgotPassword
import com.ambrxsh.buzzbuddy.fragments.FragmentLogin
import com.ambrxsh.buzzbuddy.fragments.FragmentRegister
import com.ambrxsh.buzzbuddy.utils.SessionStore
import com.google.android.material.appbar.MaterialToolbar

class ActivityPreLogin : AppCompatActivity() {

    companion object {
        const val EXTRA_FORCE_AUTH = "forceAuth"
        const val EXTRA_START_REGISTER = "startRegister"
    }

    private lateinit var toolbar: MaterialToolbar
    private var forceAuth: Boolean = false

    fun isForceAuth(): Boolean = forceAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionStore(this)
        forceAuth = intent.getBooleanExtra(EXTRA_FORCE_AUTH, false)
        if (!forceAuth && (session.isLoggedIn() || session.hasCompletedAuthGate())) {
            com.ambrxsh.buzzbuddy.model.MainActivity.startAtHome(this)
            finish()
            return
        }

        setContentView(R.layout.activity_prelogin)
        toolbar = findViewById(R.id.prelogin_toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        supportFragmentManager.addOnBackStackChangedListener { applyToolbar() }

        if (savedInstanceState == null) {
            if (intent.getBooleanExtra(EXTRA_START_REGISTER, false)) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, FragmentRegister())
                    .commit()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, FragmentLogin())
                    .commit()
            }
        }
        applyToolbar()
    }

    fun continueWithoutAccount() {
        SessionStore(this).markAuthGateCompleted()
        com.ambrxsh.buzzbuddy.model.MainActivity.startAtHome(this)
        finish()
    }

    fun showLogin(clearStack: Boolean = true) {
        if (clearStack) {
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, FragmentLogin())
                .commit()
        } else {
            supportFragmentManager.popBackStack()
        }
        applyToolbar()
    }

    fun showRegister() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, FragmentRegister())
            .addToBackStack("register")
            .commit()
        applyToolbar()
    }

    fun showForgotPassword() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, FragmentForgotPassword())
            .addToBackStack("forgot")
            .commit()
        applyToolbar()
    }

    private fun applyToolbar() {
        val entry = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        when (entry) {
            is FragmentRegister -> {
                toolbar.title = getString(R.string.register_title)
                toolbar.setNavigationIcon(R.drawable.ic_back)
            }
            is FragmentForgotPassword -> {
                toolbar.title = getString(R.string.forgot_password_title)
                toolbar.setNavigationIcon(R.drawable.ic_back)
            }
            else -> {
                toolbar.title = getString(R.string.login_title)
                if (forceAuth) {
                    toolbar.setNavigationIcon(R.drawable.ic_back)
                } else {
                    toolbar.navigationIcon = null
                }
            }
        }
    }
}
