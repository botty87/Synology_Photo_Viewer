package com.botty.photoviewer.activities.settings

import androidx.lifecycle.ViewModel
import com.botty.photoviewer.data.Settings
import com.botty.photoviewer.di.repos.GalleriesRepo

class SettingsActivityViewModel(private val settings: Settings, private val galleriesRepo: GalleriesRepo): ViewModel() {
    var showPicInfoFullScreen: Boolean
        get() = settings.showPicInfoFullScreen
        set(value) { settings.showPicInfoFullScreen = value }

    var presentationTimeout: Int
        get() = settings.presentationTimeout
        set(value) { settings.presentationTimeout = value}

    var dbMode: Boolean
        get() = settings.dbMode
        set(value) { settings.dbMode = value }

    val galleries = galleriesRepo.galleriesLiveData

    fun removeGallery(index: Int) {
        galleries.value?.get(index)?.run { galleriesRepo.removeGallery(this) }
    }
}