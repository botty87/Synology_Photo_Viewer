package com.botty.photoviewer.activities.settings

import androidx.lifecycle.ViewModel
import com.botty.photoviewer.dataRepositories.Settings
import com.botty.photoviewer.dataRepositories.localDB.DBFilesRepo
import com.botty.photoviewer.dataRepositories.localDB.DBFoldersRepo
import com.botty.photoviewer.dataRepositories.localDB.GalleriesRepo
import org.koin.core.KoinComponent
import org.koin.core.inject

class SettingsViewModel(private val settings: Settings, private val galleriesRepo: GalleriesRepo): ViewModel(), KoinComponent {
    private val dbFilesRepo: DBFilesRepo by inject()
    private val dbFoldersRepo: DBFoldersRepo by inject()

    var showPicInfoFullScreen: Boolean
        get() = settings.showPicInfoFullScreen
        set(value) { settings.showPicInfoFullScreen = value }

    var presentationTimeout: Int
        get() = settings.presentationTimeout
        set(value) { settings.presentationTimeout = value}

    var dbMode: Boolean
        get() = settings.dbMode
        set(value) { settings.dbMode = value }

    var dailyScan: Boolean
        get() = settings.dailyScan
        set(value) { settings.dailyScan = value }

    val galleries = galleriesRepo.galleriesLiveData

    fun removeGallery(index: Int) {
        galleries.value?.get(index)?.run {
            galleriesRepo.removeGallery(this)
            dbFilesRepo.removeGalleryFiles(id)
            dbFoldersRepo.removeGalleryFolders(id)
        }
    }
}