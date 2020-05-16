package com.botty.photoviewer.activities.galleryViewer.galleryView

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.botty.photoviewer.activities.galleryViewer.CacheMetadata
import com.botty.photoviewer.activities.galleryViewer.loader.GalleryContainer
import com.botty.photoviewer.components.network.LoginManager
import com.botty.photoviewer.components.removeLast
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.connectionContainers.SessionParams
import com.botty.photoviewer.data.remoteFolder.PictureContainer
import com.botty.photoviewer.data.remoteFolder.RemoteItem
import com.botty.photoviewer.di.repos.FoldersRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject
import org.koin.core.parameter.parametersOf

class GalleryViewModel (private val gallCont: GalleryContainer, val gallery: Gallery, private val foldersRepo: FoldersRepo): ViewModel(), KoinComponent {

    val actualPath = mutableListOf<String>()
    val folders = MutableLiveData<List<RemoteItem>>()
    private val loginManager: LoginManager by inject { parametersOf(gallery.connectionParams.target) }

    val pictures: MutableLiveData<List<PictureContainer>>
        get() = gallCont.pictures

    val currentGalleryPath: String
        get() = gallCont.currentGalleryPath

    val picturesMetaCache = CacheMetadata(200, gallCont.pictures)

    lateinit var sessionParams: SessionParams
        private set

    suspend fun performLoginAndLoadFolder() {
        withContext(Dispatchers.IO) {
            sessionParams = loginManager.login()
            gallCont.network = get{ parametersOf(sessionParams) }
        }
        loadFolderContent(gallery.path)
    }

    private suspend fun loadFolderContent(path: String, galleryNameToAdd: String? = null) {
        suspend fun getPictureFileHash(picName: String) = withContext(Dispatchers.Default) {
            "${gallCont.currentGalleryPath}/$picName".hashCode()
        }

        foldersRepo.loadFolderContent(path).let { folderContent ->
            gallCont.currentGalleryPath = path
            galleryNameToAdd?.run { actualPath.add(this) } ?: actualPath.removeLast()

            withContext(Dispatchers.Default) {
                folders.postValue(folderContent.folders)

                folderContent.pictures.map { picture ->
                    val hash = getPictureFileHash(picture.name)
                    PictureContainer(picture.name, hash)
                }.run {
                    gallCont.pictures.postValue(this)
                }
            }
        }
    }

    suspend fun onFolderClick(folder: RemoteItem) {
        loadFolderContent(folder.path, folder.name)
    }

    suspend fun onParentClick() {
        if(actualPath.size <= 1) {
            loadFolderContent(gallery.path)
        } else {
            var path = "${gallery.path}/"
            for(i in 0 until actualPath.size - 1) {
                path += "${actualPath[i]}/"
            }
            path = path.dropLast(1)
            loadFolderContent(path)
        }
    }
}