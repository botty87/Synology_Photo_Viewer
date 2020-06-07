package com.botty.photoviewer.activities.galleryViewer.galleryView

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.botty.photoviewer.activities.galleryViewer.CacheMetadata
import com.botty.photoviewer.activities.galleryViewer.loader.GalleryContainer
import com.botty.photoviewer.components.NoPathTreeException
import com.botty.photoviewer.components.workers.GalleryFolderScanWorker
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.SimpleItem
import com.botty.photoviewer.data.connectionContainers.SessionParams
import com.botty.photoviewer.data.remoteFolder.FolderContent
import com.botty.photoviewer.data.remoteFolder.PictureContainer
import com.botty.photoviewer.dataRepositories.FoldersRepo
import com.botty.photoviewer.dataRepositories.Settings
import com.botty.photoviewer.dataRepositories.localDB.FoldersRepoDB
import com.botty.photoviewer.dataRepositories.remote.LoginManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject
import org.koin.core.parameter.parametersOf

class GalleryViewModel (private val gallCont: GalleryContainer, val gallery: Gallery,
                        private val foldersRepo: FoldersRepo, private val androidContext: Context
): ViewModel(), KoinComponent {

    val folders = MutableLiveData<List<SimpleItem>>()
    private val loginManager: LoginManager by inject { parametersOf(gallery.connectionParams.target) }

    val pictures: MutableLiveData<List<PictureContainer>>
        get() = gallCont.pictures

    val currentGalleryPath: String
        get() = gallCont.currentGalleryPath

    //It works only in DB mode. Otherwise return always 0
    val currentFolderId: Long
        get() {
            return if(foldersRepo is FoldersRepoDB) {
                foldersRepo.currentFolderId
            } else {
                0L
            }
        }

    val pathTree: MutableList<String>
        get() = foldersRepo.pathTree ?: throw NoPathTreeException()

    val picturesMetaCache = CacheMetadata(200, gallCont.pictures)

    val settings: Settings by inject()

    lateinit var sessionParams: SessionParams
        private set

    private val folderScanLiveData = WorkManager.getInstance(androidContext).getWorkInfosByTagLiveData(GalleryFolderScanWorker.TAG)
    private val syncObserver = Observer<List<WorkInfo>> { worksInfo ->
        viewModelScope.launch(Dispatchers.Default) {
            val foldersRepoDB = foldersRepo as FoldersRepoDB
            val folderWorksInfo = worksInfo.filter { workInfo ->
                if(workInfo.state != WorkInfo.State.SUCCEEDED) {
                    return@filter false
                }
                val folderId = workInfo.outputData.getLong(GalleryFolderScanWorker.FOLDER_ID, 0L)
                return@filter foldersRepoDB.currentFolderId == folderId
            }

            if(folderWorksInfo.isEmpty()) {
                return@launch
            }

            var mostRecentTime = 0L
            lateinit var folderWork: WorkInfo
            folderWorksInfo.forEach{ workInfo ->
                val time = workInfo.outputData.getLong(GalleryFolderScanWorker.TIME_TAG, 0L)
                if(time >= mostRecentTime) {
                    mostRecentTime = time
                    folderWork = workInfo
                }
            }

            val folderToUpdate = folderWork.outputData.getBoolean(GalleryFolderScanWorker.FOLDER_TO_UPDATE_KEY, false)
            val filesToUpdate = folderWork.outputData.getBoolean(GalleryFolderScanWorker.FILES_TO_UPDATE_KEY, false)
            if(folderToUpdate || filesToUpdate) {
                foldersRepoDB.reloadCurrentFolder().run {
                    onFolderContent(this, folderToUpdate, filesToUpdate)
                }
            }

            WorkManager.getInstance(androidContext).pruneWork()
        }
    }

    init {
        folderScanLiveData.observeForever(syncObserver)
    }

    suspend fun performLoginAndLoadFolder(): Long {
        withContext(Dispatchers.IO) {
            sessionParams = loginManager.login()
            gallCont.network = get{ parametersOf(sessionParams) }
        }
        //loadFolderContent(gallery.path)
        foldersRepo.loadCurrentFolder().run {
            onFolderContent(this)
        }
        return currentFolderId
    }

    private suspend fun onFolderContent(folderContent: FolderContent, postFolders: Boolean = true, postPicture: Boolean = true) {
        gallCont.currentGalleryPath = folderContent.folderPath
        if(postFolders) {
            folders.postValue(folderContent.folders)
        }
        if(postPicture) {
            folderContent.pictures.map { picture ->
                val hash = getPictureFileHash(picture.name)
                PictureContainer(picture.name, hash)
            }.run {
                gallCont.pictures.postValue(this)
            }
        }
    }

    private suspend fun getPictureFileHash(picName: String) = withContext(Dispatchers.Default) {
        "${gallCont.currentGalleryPath}/$picName".hashCode()
    }

    suspend fun onFolderClick(folder: SimpleItem) {
        foldersRepo.loadChildFolders(folder.name).run {
            onFolderContent(this)
        }
    }

    suspend fun onParentClick() {
        foldersRepo.loadParentFolder().run {
            onFolderContent(this)
        }
    }

    override fun onCleared() {
        folderScanLiveData.removeObserver(syncObserver)
        super.onCleared()
    }
}