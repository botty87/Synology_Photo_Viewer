package com.botty.photoviewer.activities.galleryViewer.galleryView

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.botty.photoviewer.activities.galleryViewer.CacheMetadata
import com.botty.photoviewer.activities.galleryViewer.loader.GalleryContainer
import com.botty.photoviewer.components.network.LoginManager
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.SimpleItem
import com.botty.photoviewer.data.connectionContainers.SessionParams
import com.botty.photoviewer.data.remoteFolder.FolderContent
import com.botty.photoviewer.data.remoteFolder.PictureContainer
import com.botty.photoviewer.di.repos.FoldersRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject
import org.koin.core.parameter.parametersOf

class GalleryViewModel (private val gallCont: GalleryContainer, val gallery: Gallery, private val foldersRepo: FoldersRepo): ViewModel(), KoinComponent {

    val folders = MutableLiveData<List<SimpleItem>>()
    private val loginManager: LoginManager by inject { parametersOf(gallery.connectionParams.target) }

    val pictures: MutableLiveData<List<PictureContainer>>
        get() = gallCont.pictures

    val currentGalleryPath: String
        get() = gallCont.currentGalleryPath

    val pathTree: MutableList<String>
        get() = foldersRepo.pathTree

    val picturesMetaCache = CacheMetadata(200, gallCont.pictures)

    lateinit var sessionParams: SessionParams
        private set

    suspend fun performLoginAndLoadFolder() {
        withContext(Dispatchers.IO) {
            sessionParams = loginManager.login()
            gallCont.network = get{ parametersOf(sessionParams) }
        }
        //loadFolderContent(gallery.path)
        foldersRepo.loadCurrentFolder().run {
            onFolderContent(this)
        }
    }

    private suspend fun onFolderContent(folderContent: FolderContent) {
        gallCont.currentGalleryPath = folderContent.folderPath
        folders.postValue(folderContent.folders)
        folderContent.pictures.map { picture ->
            val hash = getPictureFileHash(picture.name)
            PictureContainer(picture.name, hash)
        }.run {
            gallCont.pictures.postValue(this)
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
}