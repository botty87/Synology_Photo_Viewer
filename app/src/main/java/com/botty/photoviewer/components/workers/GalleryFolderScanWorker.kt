package com.botty.photoviewer.components.workers

import android.content.Context
import androidx.work.*
import com.botty.photoviewer.components.isNotNull
import com.botty.photoviewer.data.fileStructure.BaseMedia
import com.botty.photoviewer.data.fileStructure.MediaFile
import com.botty.photoviewer.data.fileStructure.MediaFolder
import com.botty.photoviewer.data.remoteFolder.FolderContent
import com.botty.photoviewer.dataRepositories.localDB.DBFoldersRepo
import com.botty.photoviewer.dataRepositories.localDB.GalleriesRepo
import com.botty.photoviewer.dataRepositories.remote.FoldersRepoNet
import com.botty.photoviewer.dataRepositories.remote.LoginManager
import com.botty.photoviewer.dataRepositories.remote.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.util.concurrent.TimeUnit

class GalleryFolderScanWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params), KoinComponent {

    private val dbFoldersRepo: DBFoldersRepo by inject()
    private val galleriesRepo: GalleriesRepo by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if(runAttemptCount > 3) {
            val errMessage = "too many failed, give up"
            Timber.d(errMessage)
            return@withContext Result.failure(workDataOf(ScanGalleriesWorker.ERROR_KEY to (errMessage)))
        }

        val folderId = inputData.getLong(FOLDER_ID, 0L)
        if(folderId == 0L) {
            return@withContext Result.failure()
        }

        val folder = dbFoldersRepo.getFolder(folderId)
        var path = ""
        var parentFolder: MediaFolder? = folder
        while (parentFolder.isNotNull()) {
            path = "/${parentFolder!!.name}" + path
            parentFolder = parentFolder.parentFolder.target
        }

        val network = getNetwork(folder)
        val foldersRepoNet = get<FoldersRepoNet>{ parametersOf(network, null) }
        val result = checkFolderContent(foldersRepoNet.loadFolderPath(path), folder)

        val resultData = workDataOf(
            FOLDER_ID to folderId,
            FOLDER_TO_UPDATE_KEY to result.foldersToUpdate,
            FILES_TO_UPDATE_KEY to result.filesToUpdate,
            TIME_TAG to System.currentTimeMillis()
        )

        return@withContext Result.success(resultData)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun checkFolderContent(remoteFolder: FolderContent, dbFolder: MediaFolder): CheckResult = withContext(Dispatchers.Default) {
        fun filterList(removedItems: MutableList<BaseMedia>, newItemsName: MutableList<String>, remoteItems: List<String>) {
            remoteLoop@ for(remoteItemName in remoteItems) {
                for(removedItem in removedItems) {
                    if(removedItem.name == remoteItemName) {
                        removedItems.remove(removedItem)
                        continue@remoteLoop
                    }
                }
                newItemsName.add(remoteItemName)
            }
        }

        val foldersToRemove = dbFolder.childFolders.toMutableList() as MutableList<BaseMedia>
        val remoteFolders = remoteFolder.folders.map { it.name }
        val newFoldersName = mutableListOf<String>()
        filterList(foldersToRemove, newFoldersName, remoteFolders)
        val newFolders = newFoldersName.map { MediaFolder(name = it, galleryId = dbFolder.galleryId) }

        val filesToRemove = dbFolder.childFiles.toMutableList() as MutableList<BaseMedia>
        val remoteFiles = remoteFolder.pictures.map { it.name }
        val newFilesName = mutableListOf<String>()
        filterList(filesToRemove, newFilesName, remoteFiles)
        val newFiles = newFilesName.map { MediaFile(name = it, galleryId = dbFolder.galleryId) }

        dbFoldersRepo.updateFolder(dbFolder, newFolders, (foldersToRemove as List<MediaFolder>),
            newFiles, (filesToRemove as List<MediaFile>))

        val foldersToUpdate = foldersToRemove.isNotEmpty() || newFolders.isNotEmpty()
        val filesToUpdate = filesToRemove.isNotEmpty() || newFiles.isNotEmpty()
        CheckResult(foldersToUpdate, filesToUpdate)
    }

    private suspend fun getNetwork(folder: MediaFolder): Network {
        val gallery = galleriesRepo.getGallery(folder.galleryId)
        val loginManager: LoginManager = get { parametersOf(gallery.connectionParams.target) }
        val sessionParams = loginManager.login()
        return get { parametersOf(sessionParams) }
    }

    companion object {
        const val TAG = "scan_folder_job"

        const val FOLDER_ID = "folder_id"
        const val FOLDER_TO_UPDATE_KEY = "folder_to_update"
        const val FILES_TO_UPDATE_KEY = "files_to_update"
        const val TIME_TAG = "time"

        fun setWorker(context: Context, folderId: Long) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = workDataOf(FOLDER_ID to folderId)

            OneTimeWorkRequestBuilder<GalleryFolderScanWorker>()
                .addTag(TAG)
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
                .run {
                    WorkManager.getInstance(context).enqueue(this)
                }
        }
    }

    private data class CheckResult(val foldersToUpdate: Boolean, val filesToUpdate: Boolean)
}