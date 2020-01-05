package com.botty.photoviewer.tools.workers.scanGalleries

import android.content.Context
import androidx.work.*
import com.botty.photoviewer.data.ObjectBox
import com.botty.photoviewer.data.SessionParams
import com.botty.photoviewer.data.fileStructure.MediaFile
import com.botty.photoviewer.data.fileStructure.MediaFolder
import com.botty.photoviewer.tools.log
import com.botty.photoviewer.tools.network.Network
import com.botty.photoviewer.tools.network.responses.containers.Share
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class ScanGalleriesWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if(ScanGalleriesPref.isGalleryOpened) {
            return@withContext Result.retry()
        }

        ScanGalleriesPref.isSyncingGalleries = true

        ObjectBox.mediaFileBox.removeAll()
        ObjectBox.mediaFolderBox.removeAll()
        val galleries = ObjectBox.galleryBox.all

        val jobs = galleries.map { gallery ->
            async(Dispatchers.IO) {
                val sid = Network.login(gallery.connectionParams.target).sid
                val sessionParams = gallery.connectionParams.target.toSessionParams(sid)
                gallery.folder.target = MediaFolder(name = gallery.name)
                ObjectBox.galleryBox.put(gallery)
                scanGallery(sessionParams, gallery.path, gallery.folder.target, true)
            }
        }

        try {
            jobs.awaitAll()
        } catch (e: Exception) {
            e.log()
            ScanGalleriesPref.isSyncingGalleries = false
            return@withContext Result.failure(workDataOf(ERROR_KEY to (e.localizedMessage ?: e.message )))
        }

        ScanGalleriesPref.isFirstSyncNeeded = false
        ScanGalleriesPref.isSyncingGalleries = false
        return@withContext Result.success()
    }

    private suspend fun scanGallery(sessionParams:SessionParams, path: String,
                                    mainFolder: MediaFolder, mainPath: Boolean) {
        fun fillDB(shares: List<Share>) {
                        val shareDirs = mutableListOf<Pair<String, MediaFolder>>()
            shares.forEach { share ->
                if(share.isNotHidden()) {
                    if (share.isdir) {
                        val mediaFolder = MediaFolder(name = share.name)
                        mainFolder.childFolders.add(mediaFolder)
                        shareDirs.add(share.path to mediaFolder)
                    } else if (share.isPicture()) {
                        mainFolder.childFiles.add(MediaFile(name = share.name))
                    }
                }
            }
            ObjectBox.mediaFolderBox.put(mainFolder)
            runBlocking {
                shareDirs.forEach { share ->
                    scanGallery(sessionParams, share.first, share.second, false)
                }
                if(mainPath) {
                    Timber.d("load folders end")
                } else {
                    Timber.d("loaded ${mainFolder.name} folder")
                }
            }
        }

        Network.getFoldersContent(sessionParams, path).run {
            fillDB(files)
        }
    }

    companion object {
        private const val TAG = "scan_galleries_job"

        const val ERROR_KEY = "scan_error"

        fun setWorker(context: Context): UUID {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val work = OneTimeWorkRequestBuilder<ScanGalleriesWorker>()
                .addTag(TAG)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, work)
            return work.id
        }
    }
}