package com.botty.photoviewer.galleryViewer.galleryView

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.botty.photoviewer.components.endsWithNoCase
import com.botty.photoviewer.components.log
import com.botty.photoviewer.components.network.LoginManager
import com.botty.photoviewer.components.network.responses.containers.Share
import com.botty.photoviewer.components.removeLast
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.PictureContainer
import com.botty.photoviewer.data.connectionContainers.SessionParams
import com.botty.photoviewer.galleryViewer.CacheMetadata
import com.botty.photoviewer.galleryViewer.loader.GalleryContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject
import org.koin.core.parameter.parametersOf
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class GalleryViewModel (private val gallCont: GalleryContainer, val gallery: Gallery): ViewModel(), KoinComponent {

    val actualPath = mutableListOf<String>()
    val folders = MutableLiveData<List<Share>>()
    private val loginManager: LoginManager by inject { parametersOf(gallery.connectionParams.target) }

    val pictures: MutableLiveData<List<PictureContainer>>
        get() = gallCont.pictures

    val currentGalleryPath: String
        get() = gallCont.currentGalleryPath

    val picturesMetaCache = CacheMetadata(200, gallCont.pictures)

    lateinit var sessionParams: SessionParams
        private set

    suspend fun performLoginAndLoadFolder() = suspendCoroutine<Void?> { continuation ->
        //TODO remove launch?
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { loginManager.login() }
            }.onSuccess { sessionParams ->
                this@GalleryViewModel.sessionParams = sessionParams
                gallCont.network = get{ parametersOf(sessionParams) }
                loadFolderContent(gallery.path)
                continuation.resume(null)
            }.onFailure { e ->
                e.log()
                continuation.resumeWithException(e)
            }
        }
    }

    private suspend fun loadFolderContent(path: String, galleryNameToAdd: String? = null) {
        fun Share.isPicture(): Boolean = name.endsWithNoCase(".webp") ||
                name.endsWithNoCase(".jpg") ||
                name.endsWithNoCase(".jpeg") ||
                name.endsWithNoCase(".png") ||
                name.endsWithNoCase(".tif") ||
                name.endsWithNoCase(".tiff") ||
                name.endsWithNoCase(".gif")

        suspend fun getPictureFileHash(picName: String) = withContext(Dispatchers.Default) {
            "${gallCont.currentGalleryPath}/$picName".hashCode()
        }

        withContext(Dispatchers.IO) {
            runCatching {
                gallCont.network.getFoldersContent(path)
            }.onSuccess { response ->
                gallCont.currentGalleryPath = path

                withContext (Dispatchers.Default) {
                    galleryNameToAdd?.run { actualPath.add(this) } ?: actualPath.removeLast()

                    //Create and set the folders
                    response.files.filter { file ->
                        file.isdir && file.isNotHidden()
                    }.run { folders.postValue(this) }

                    //Create and set the pictures
                    response.files.filter { file ->
                        file.isPicture() && file.isNotHidden()
                    }.map { share ->
                        val hash = getPictureFileHash(share.name)
                        PictureContainer(share.name, hash)
                    }.run { gallCont.pictures.postValue(this) }
                }
            }
        }
    }

    suspend fun onFolderClick(folder: Share) {
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