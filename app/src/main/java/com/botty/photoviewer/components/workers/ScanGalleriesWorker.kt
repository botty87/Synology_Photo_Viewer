package com.botty.photoviewer.components.workers

import android.content.Context
import androidx.work.*
import com.botty.photoviewer.MyApplication
import com.botty.photoviewer.components.log
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.fileStructure.MediaFile
import com.botty.photoviewer.data.fileStructure.MediaFolder
import com.botty.photoviewer.data.remoteFolder.FolderContent
import com.botty.photoviewer.data.remoteFolder.RemoteItem
import com.botty.photoviewer.dataRepositories.localDB.DBFilesRepo
import com.botty.photoviewer.dataRepositories.localDB.DBFoldersRepo
import com.botty.photoviewer.dataRepositories.localDB.GalleriesRepo
import com.botty.photoviewer.dataRepositories.remote.FoldersRepoNet
import com.botty.photoviewer.dataRepositories.remote.LoginManager
import com.botty.photoviewer.dataRepositories.remote.Network
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

        if(runAttemptCount > 3) {
            val errMessage = "too many failed, give up"
            Timber.d(errMessage)
            return@withContext Result.failure(workDataOf(ERROR_KEY to (errMessage)))
        }

        if((applicationContext as MyApplication).isGalleryViewerOpened) {
            return@withContext Result.retry()
        }

        val galleries = galleriesRepo.galleriesWithNoSync

        setProgress(workDataOf(
            DONE_GALLERIES to 0,
            TOTAL_GALLERIES to galleries.size
        ))

        val galleriesDone = AtomicInteger(0)
        val jobs = galleries.map { gallery ->
            async(Dispatchers.IO) {
                val network = getNetwork(gallery)
                //val foldersRepoNet = get<FoldersRepoNet>{ parametersOf(network, gallery) }
                 val foldersRepoNet = get<FoldersRepoNet>{ parametersOf(network, null) } //TODO test

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

        const val DONE_GALLERIES = "done_GAL"
        const val TOTAL_GALLERIES = "tot_gal"

        const val ERROR_KEY = "scan_error"

        fun setWorker(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            OneTimeWorkRequestBuilder<ScanGalleriesWorker>()
                .addTag(TAG)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
                .run {
                    WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, this)
                }
        }

        fun cancelWorks(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        }
    }
}