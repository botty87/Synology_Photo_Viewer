package com.botty.photoviewer

import android.app.Application
import android.content.res.Resources
import com.botty.photoviewer.data.ObjectBox
import io.objectbox.android.AndroidObjectBrowser
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.android.gms.ads.MobileAds
import timber.log.Timber
import timber.log.Timber.DebugTree

class MyApplication: Application() {
    companion object {
        private lateinit var myResources: Resources

        fun getString(stringRes: Int): String = myResources.getString(stringRes)
    }

    override fun onCreate() {
        super.onCreate()
        myResources = resources
        ObjectBox.init(this)
        //ca-app-pub-3940256099942544~3347511713 TEST
        MobileAds.initialize(this, "ca-app-pub-9694877750002081~5509085931")
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
            val started = AndroidObjectBrowser(ObjectBox.boxStore).start(this)
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
                Crashlytics.logException(e)
            }
            Crashlytics.logException(t)
        }
    }
}