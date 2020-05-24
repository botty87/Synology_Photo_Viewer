package com.botty.photoviewer.components.workers

import android.content.Context
import androidx.work.*
import com.botty.photoviewer.components.log
import com.botty.photoviewer.components.network.FoldersRepoNet
import com.botty.photoviewer.components.network.LoginManager
import com.botty.photoviewer.components.network.Network
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.fileStructure.MediaFile
import com.botty.photoviewer.data.fileStructure.MediaFolder
import com.botty.photoviewer.data.remoteFolder.FolderContent
import com.botty.photoviewer.data.remoteFolder.RemoteItem
import com.botty.photoviewer.di.repos.DBFilesRepo
import com.botty.photoviewer.di.repos.DBFoldersRepo
import com.botty.photoviewer.di.repos.GalleriesRepo
import kotlinx.coroutines.*
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ScanGalleriesWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params), KoinComponent {

    private val galleriesRepo: GalleriesRepo by inject()
    private val dbFoldersRepo: DBFoldersRepo by inject()
    private val dbFilesRepo: DBFilesRepo by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        val galleryId = inputData.getLong(GALLERY_ID, 0L)
        val folderId = inputData.getLong(FOLDER_ID, 0L)

        val galleries = if(galleryId == 0L)
            galleriesRepo.galleries
        else
            listOf(galleriesRepo.getGallery(galleryId))

        setProgress(workDataOf(
            DONE_GALLERIES to 0,
            TOTAL_GALLERIES to galleries.size
        ))

        val galleriesDone = AtomicInteger(0)
        val jobs = galleries.map { gallery ->
            async(Dispatchers.IO) {
                val network = getNetwork(gallery)
                val foldersRepoNet = get<FoldersRepoNet>{ parametersOf(network, gallery) }

                if(folderId == 0L) {
                    dbFilesRepo.removeGalleryFiles(gallery.id)
                    dbFoldersRepo.removeGalleryFolders(gallery.id)

                    gallery.folder.target = MediaFolder(name = gallery.name, galleryId = gallery.id)
                    gallery.lastSync = null
                    galleriesRepo.saveGallery(gallery)

                    scanGallery(foldersRepoNet, gallery.path, gallery.folder.target, true)
                    gallery.lastSync = Date()
                    galleriesRepo.saveGallery(gallery)
                    setProgress(workDataOf(
                        DONE_GALLERIES to galleriesDone.incrementAndGet(),
                        TOTAL_GALLERIES to galleries.size
                    ))
                }
            }
        }

        try {
            jobs.awaitAll()
        } catch (e: Exception) {
            e.log()
            return@withContext Result.failure(workDataOf(ERROR_KEY to (e.localizedMessage ?: e.message)))
        }

        return@withContext Result.success()
    }

    private suspend fun getNetwork(gallery: Gallery): Network {
        val loginManager: LoginManager = get { parametersOf(gallery.connectionParams.target) }
        val sessionParams = loginManager.login()
        return get { parametersOf(sessionParams) }
    }

    private suspend fun scanGallery(foldersRepoNet: FoldersRepoNet, path: String,
                                    mainFolder: MediaFolder, mainPath: Boolean) {

        fun fillDB(folderContent: FolderContent, folderPath: String) {
            val subFolders = mutableListOf<Pair<String, MediaFolder>>()
            folderContent.folders.forEach { folder ->
                val mediaFolder = MediaFolder(name = folder.name, galleryId = mainFolder.galleryId)
                mainFolder.childFolders.add(mediaFolder)

                //we can perform this cast because the result is always (obviously) from the remote
                subFolders.add((folder as RemoteItem).path to mediaFolder)
            }

            folderContent.pictures.forEach { picture ->
                mainFolder.childFiles.add(MediaFile(name = picture.name, galleryId = mainFolder.galleryId))
            }

            dbFoldersRepo.saveFolder(mainFolder)

            runBlocking {
                subFolders.forEach{ folder ->
                    scanGallery(foldersRepoNet, folder.first, folder.second, false)
                }
                if(mainPath) {
                    Timber.d("load folders end")
                } else {
                    Timber.d("loaded ${mainFolder.name} folder")
                }
            }
        }

        fillDB(foldersRepoNet.loadFolderPath(path), path)
    }

    companion object {
        const val TAG = "scan_galleries_job"
        private const val GALLERY_ID = "gallery_id"
        const val FOLDER_ID = "folder_id"
        //private const val DAILY_SYNC_TAG = "dailysync_tag"

        const val DONE_GALLERIES = "done_GAL"
        const val TOTAL_GALLERIES = "tot_gal"

        const val ERROR_KEY = "scan_error"

        fun setWorker(context: Context, galleryId: Long = 0L, folderId: Long = 0L): UUID {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = workDataOf(GALLERY_ID to galleryId, FOLDER_ID to folderId)

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