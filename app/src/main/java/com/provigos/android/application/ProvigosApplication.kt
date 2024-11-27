package com.provigos.android.application

import android.app.Application
import com.facebook.stetho.Stetho
import com.provigos.android.modules.coreModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidFileProperties
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.fragment.koin.fragmentFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class ProvigosApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        initTimber()
        initKoin()
        initStetho()
    }

    private fun initTimber() {
        Timber.plant(Timber.DebugTree())
    }

    private fun initKoin() {

        val modules = listOf(
            coreModule
        )

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@ProvigosApplication)
            androidFileProperties()
            fragmentFactory()
            modules(modules)
        }
    }

    private fun initStetho() {
        Stetho.initializeWithDefaults(this)
    }
}