package com.botty.photoviewer.tools.workers.scanGalleries

import android.content.Context
import androidx.work.*
import com.botty.photoviewer.data.ObjectBox
import com.botty.photoviewer.data.SessionParams
import com.botty.photoviewer.data.fileStructure.MediaFile
import com.botty.photoviewer.data.fileStructure.MediaFile_
import com.botty.photoviewer.data.fileStructure.MediaFolder
import com.botty.photoviewer.data.fileStructure.MediaFolder_
import com.botty.photoviewer.tools.isNotNull
import com.botty.photoviewer.tools.log
import com.botty.photoviewer.tools.network.Network
import com.botty.photoviewer.tools.network.responses.containers.Share
import io.objectbox.kotlin.query
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class ScanGalleriesWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val dailySync = inputData.getBoolean(DAILY_SYNC_TAG, false)
        if(ScanGalleriesPref.isGalleryOpened && dailySync) {
            return@withContext Result.retry()
        }

        val galleryId = inputData.getLong(GALLERY_ID, 0L)
        val folderId = inputData.getLong(FOLDER_ID, 0L)

        val mediaFileBox = ObjectBox.mediaFileBox
        val mediaFolderBox = ObjectBox.mediaFolderBox

        val galleries = if(galleryId == 0L)
            ObjectBox.galleryBox.all
        else
            listOf(ObjectBox.galleryBox[galleryId])

        val jobs = galleries.map { gallery ->
            async(Dispatchers.IO) {
                val sid = Network.login(gallery.connectionParams.target).sid
                val sessionParams = gallery.connectionParams.target.toSessionParams(sid)

                if(folderId == 0L) {
                    mediaFileBox.query {
                        equal(MediaFile_.galleryId, gallery.id)
                    }.remove()

                    mediaFolderBox.query {
                        equal(MediaFolder_.galleryId, gallery.id)
                    }.remove()

                    gallery.folder.target = MediaFolder(name = gallery.name, galleryId = gallery.id)
                    ObjectBox.galleryBox.put(gallery)
                    scanGallery(sessionParams, gallery.path, gallery.folder.target, true)
                } else {
                    var parentFolder = mediaFolderBox[folderId]

                    var path = ""
                    while (parentFolder.parentFolder.target.isNotNull()) {
                        path = "/${parentFolder.name}$path"
                        parentFolder = parentFolder.parentFolder.target
                    }

                    path = "${gallery.path}$path"

                    val foldersIdToRemove = mutableListOf<Long>()
                    val filesIdToRemove = mutableListOf<Long>()

                    fun removeFolderAndSubContent(folderIdToClean: Long) {
                        mediaFolderBox[folderIdToClean].run {
                            val tempFold = this
                            childFiles.forEach { mediaFile ->
                                filesIdToRemove.add(mediaFile.id)
                            }
                            childFolders.forEach { mediaFolder ->
                                foldersIdToRemove.add(mediaFolder.id)
                                removeFolderAndSubContent(mediaFolder.id)
                            }
                        }
                    }

                    removeFolderAndSubContent(folderId)
                    mediaFolderBox.remove(*foldersIdToRemove.toLongArray())
                    mediaFileBox.remove(*filesIdToRemove.toLongArray())

                    scanGallery(sessionParams, path, mediaFolderBox[folderId], true)
                }
            }
        }

        try {
            jobs.awaitAll()
        } catch (e: Exception) {
            e.log()
            return@withContext Result.failure(
                workDataOf(
                    ERROR_KEY to (e.localizedMessage ?: e.message)
                )
            )
        }

        ScanGalleriesPref.isFirstSyncNeeded = false

        return@withContext Result.success()
    }

    private suspend fun scanGallery(sessionParams:SessionParams, path: String,
                                    mainFolder: MediaFolder, mainPath: Boolean) {
        fun fillDB(shares: List<Share>) {
                        val shareDirs = mutableListOf<Pair<String, MediaFolder>>()
            shares.forEach { share ->
                if(share.isNotHidden()) {
                    if (share.isdir) {
                        val mediaFolder = MediaFolder(name = share.name, galleryId = mainFolder.galleryId)
                        mainFolder.childFolders.add(mediaFolder)
                        shareDirs.add(share.path to mediaFolder)
                    } else if (share.isPicture()) {
                        mainFolder.childFiles.add(MediaFile(name = share.name, galleryId = mainFolder.galleryId))
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
        const val TAG = "scan_galleries_job"
        private const val GALLERY_ID = "gallery_id"
        private const val FOLDER_ID = "folder_id"
        private const val DAILY_SYNC_TAG = "dailysync_tag"

        const val ERROR_KEY = "scan_error"

        fun setWorker(context: Context, galleryId: Long, folderId: Long = 0L, dailySync: Boolean = false): UUID {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = workDataOf(GALLERY_ID to galleryId,
                FOLDER_ID to folderId,
                DAILY_SYNC_TAG to dailySync)

            val work = OneTimeWorkRequestBuilder<ScanGalleriesWorker>()
                .addTag(TAG)
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, work)
            return work.id
        }
    }
}