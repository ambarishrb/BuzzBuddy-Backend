package com.ambrxsh.buzzbuddy

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.ambrxsh.buzzbuddy.clients.AuthClientService
import com.ambrxsh.buzzbuddy.network.AuthInterceptor
import com.ambrxsh.buzzbuddy.network.TokenAuthenticator
import com.ambrxsh.buzzbuddy.scheduler.AlarmRescheduler
import com.ambrxsh.buzzbuddy.utils.SessionStore
import com.ambrxsh.buzzbuddy.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

class BuzzBuddyApp : Application() {

    lateinit var retrofit: Retrofit
        private set

    lateinit var session: SessionStore
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        session = SessionStore(this)

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val baseClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val refreshRetrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(baseClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val refreshService = refreshRetrofit.create(AuthClientService::class.java)

        val authedClient = baseClient.newBuilder()
            .addInterceptor(AuthInterceptor(session))
            .authenticator(TokenAuthenticator(session, refreshService, ::expireSession))
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(authedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        if (AlarmRescheduler.isUserUnlocked(this)) {
            SettingsManager(this).loadSettings()
        }
        appScope.launch {
            try {
                AlarmRescheduler.rescheduleAll(this@BuzzBuddyApp)
            } catch (e: Exception) {
                Timber.e(e, "Startup reschedule failed")
            }
        }
    }

    private fun expireSession() {
        session.clear()
        session.markAuthGateCompleted()
        Handler(Looper.getMainLooper()).post {
            startActivity(
                Intent(this, com.ambrxsh.buzzbuddy.model.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }
    }
}
