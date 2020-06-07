package com.botty.photoviewer.activities.main

import android.content.Context
import androidx.lifecycle.*
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.botty.photoviewer.components.workers.ScanGalleriesWorker
import com.botty.photoviewer.components.workers.ScanGalleriesWorker.Companion.DONE_GALLERIES
import com.botty.photoviewer.components.workers.ScanGalleriesWorker.Companion.TOTAL_GALLERIES
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.SyncStatus
import com.botty.photoviewer.dataRepositories.Settings
import com.botty.photoviewer.dataRepositories.localDB.DBFilesRepo
import com.botty.photoviewer.dataRepositories.localDB.DBFoldersRepo
import com.botty.photoviewer.dataRepositories.localDB.GalleriesRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject

class MainViewModel(private val galleriesRepo: GalleriesRepo, private val applicationContext: Context) : ViewModel(), KoinComponent {
    val galleries: LiveData<List<Gallery>> = galleriesRepo.galleriesLiveData

    private val settings: Settings by inject()
    private val dbFilesRepo: DBFilesRepo by inject()
    private val dbFoldersRepo: DBFoldersRepo by inject()

    val syncStatus = MutableLiveData(SyncStatus(false))
    private val syncLiveData = WorkManager.getInstance(applicationContext).getWorkInfosByTagLiveData(ScanGalleriesWorker.TAG)
    private val syncObserver = Observer<List<WorkInfo>> { worksInfo ->
        worksInfo.lastOrNull()?.let { workInfo ->
            if(workInfo.state.isFinished) {
                //Error is present only on failure. Otherwise is null, and it is fine!
                val errorMessage = workInfo.outputData.getString(ScanGalleriesWorker.ERROR_KEY)
                syncStatus.postValue(SyncStatus(false, errorMessage = errorMessage))
            } else {
                if(workInfo.state == WorkInfo.State.RUNNING) {
                    val totalGalleries = workInfo.progress.getInt(TOTAL_GALLERIES, 0)
                    val doneGalleries = workInfo.progress.getInt(DONE_GALLERIES, 0)
                    syncStatus.postValue(SyncStatus(doneGalleries, totalGalleries))
                } else {
                    syncStatus.postValue(SyncStatus(false))
                }
            }
        }
    }

    init {
        syncLiveData.observeForever(syncObserver)
    }

    fun checkDBSyncStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            when {
                !settings.dbMode -> {
                    ScanGalleriesWorker.cancelWorks(applicationContext)
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

    private fun startSyncGalleries() {
        ScanGalleriesWorker.setWorker(applicationContext)
    }

    override fun onCleared() {
        syncLiveData.removeObserver(syncObserver)
        super.onCleared()
    }
}