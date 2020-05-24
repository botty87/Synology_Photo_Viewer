package com.botty.photoviewer.activities.main

import android.content.Context
import androidx.lifecycle.*
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.botty.photoviewer.components.workers.ScanGalleriesWorker
import com.botty.photoviewer.components.workers.ScanGalleriesWorker.Companion.DONE_GALLERIES
import com.botty.photoviewer.components.workers.ScanGalleriesWorker.Companion.TOTAL_GALLERIES
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.Settings
import com.botty.photoviewer.data.SyncStatus
import com.botty.photoviewer.di.repos.DBFilesRepo
import com.botty.photoviewer.di.repos.DBFoldersRepo
import com.botty.photoviewer.di.repos.GalleriesRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject

class MainViewModel(private val galleriesRepo: GalleriesRepo, private val androidContext: Context) : ViewModel(), KoinComponent {
    val galleries: LiveData<List<Gallery>> = galleriesRepo.galleriesLiveData

    private val settings: Settings by inject()
    private val dbFilesRepo: DBFilesRepo by inject()
    private val dbFoldersRepo: DBFoldersRepo by inject()

    private var syncLiveDataObserver: Pair<LiveData<WorkInfo>, Observer<WorkInfo>>? = null

    val syncStatus = MutableLiveData(SyncStatus(false))

    fun checkDBSyncStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            when {
                !settings.dbMode -> {
                    dbFilesRepo.removeAllFiles()
                    dbFoldersRepo.removeAllFolders()
                    galleriesRepo.galleries.apply {
                        forEach { gallery ->
                            gallery.lastSync = null
                        }
                    }.run { galleriesRepo.saveGalleries(this) }
                    syncStatus.postValue(SyncStatus(false))
                }
                galleriesRepo.hasGalleryToSync -> {
                    startSyncGalleries()
                }
                else -> {
                    syncStatus.postValue(SyncStatus(false))
                }
            }
        }
    }

    private suspend fun startSyncGalleries() {
        val uuid = ScanGalleriesWorker.setWorker(androidContext)
        val liveData = WorkManager.getInstance(androidContext)
            .getWorkInfoByIdLiveData(uuid)

        val observer = Observer<WorkInfo> { workInfo ->
            if(workInfo.state.isFinished) {
                //Error is present only on failure. Otherwise is null, and it is fine!
                val errorMessage = workInfo.outputData.getString(ScanGalleriesWorker.ERROR_KEY)
                syncStatus.postValue(SyncStatus(false, errorMessage = errorMessage))
                syncLiveDataObserver?.run { first.removeObserver(second) }
                syncLiveDataObserver = null
            } else {
                val totalGalleries = workInfo.progress.getInt(TOTAL_GALLERIES, 0)
                val doneGalleries = workInfo.progress.getInt(DONE_GALLERIES, 0)
                syncStatus.postValue(SyncStatus(doneGalleries, totalGalleries))
            }
        }

        syncLiveDataObserver = Pair(liveData, observer)
        withContext(Dispatchers.Main){ liveData.observeForever(observer) }
    }

    override fun onCleared() {
        syncLiveDataObserver?.run { first.removeObserver(second) }
        super.onCleared()
    }
}