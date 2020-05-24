package com.botty.photoviewer

import android.app.Application
import android.content.res.Resources
import android.util.Log
import com.botty.photoviewer.dataRepositories.localDB.impl.ObjectBox
import com.botty.photoviewer.di.*
import com.google.android.gms.ads.MobileAds
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.objectbox.android.AndroidObjectBrowser
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.get
import timber.log.Timber
import timber.log.Timber.DebugTree

class MyApplication: Application(), KoinComponent {
    companion object {
        private lateinit var myResources: Resources

        fun getString(stringRes: Int): String = myResources.getString(stringRes)
    }

    var isGalleryViewerOpened = false

    override fun onCreate() {
        super.onCreate()
        myResources = resources

        startKoin{
            androidLogger()
            androidContext(this@MyApplication)
            modules(
                appModule,
                viewModelsModule,
                networkModule,
                dbModule,
                scopedModules
            )
        }

        MobileAds.initialize(this)
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
            val started = AndroidObjectBrowser(get<ObjectBox>().boxStore).start(this)
            Timber.i("ObjectBrowser Started: $started")
        } else {
            Timber.plant(CrashReportingTree())
        }
    }

    private class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if(priority == Log.VERBOSE || priority == Log.DEBUG) {
                return
            }
            try {
                Timber.log(priority, t, message)
            } catch (e: OutOfMemoryError) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }
            t?.run { FirebaseCrashlytics.getInstance().recordException(this) }
        }
    }
}