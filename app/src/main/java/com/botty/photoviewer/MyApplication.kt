package com.botty.photoviewer

import android.app.Application
import android.content.res.Resources
import com.botty.photoviewer.data.ObjectBox
import io.objectbox.android.AndroidObjectBrowser
import android.util.Log
import com.crashlytics.android.Crashlytics
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
            Timber.log(priority, t, message)
            Crashlytics.logException(t)
        }
    }
}