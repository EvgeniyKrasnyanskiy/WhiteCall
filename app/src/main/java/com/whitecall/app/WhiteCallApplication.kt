package com.whitecall.app

import android.app.Application
import com.whitecall.app.data.local.AppDatabase
import com.whitecall.app.data.preferences.AppPreferences
import com.whitecall.app.data.repository.CallBlockingRepository
import com.whitecall.app.data.repository.WhiteListRepository
import com.whitecall.app.domain.usecase.NormalizePhoneNumberUseCase
import com.whitecall.app.domain.usecase.ShouldBlockCallUseCase

class WhiteCallApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val preferences: AppPreferences by lazy { AppPreferences(this) }

    val normalizePhoneNumberUseCase by lazy { NormalizePhoneNumberUseCase() }
    val whiteListRepository by lazy {
        WhiteListRepository(database.whiteListDao(), database.groupDao(), normalizePhoneNumberUseCase)
    }
    val callBlockingRepository by lazy {
        CallBlockingRepository(database.blockedCallDao(), preferences)
    }
    val shouldBlockCallUseCase by lazy {
        ShouldBlockCallUseCase(
            whiteListRepository = whiteListRepository,
            preferences = preferences,
            normalizePhoneNumberUseCase = normalizePhoneNumberUseCase
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.whitecall.app.util.LocaleHelper.applyLanguage(preferences.appLanguage)
    }

    companion object {
        lateinit var instance: WhiteCallApplication
            private set
    }
}
