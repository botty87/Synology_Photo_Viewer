package com.botty.photoviewer.components.network

import com.botty.photoviewer.activities.galleryViewer.loader.GalleryContainer
import com.botty.photoviewer.components.removeLast
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.remoteFolder.FolderContent
import com.botty.photoviewer.data.remoteFolder.RemoteItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FoldersRepoNetImpl(private var galleryContainer: GalleryContainer, gallery: Gallery) : FoldersRepoNet {
    override var pathTree = mutableListOf(gallery.path)

    /*constructor(network: Network) {
        this.network = network
    }*/

    /*override suspend fun loadFolderContent(path: String): FolderContent {

        val response = withContext(Dispatchers.IO) {
            galleryContainer.network.getFoldersContent(path)
        }

        @Suppress("UNCHECKED_CAST")
        return withContext(Dispatchers.Default) {
            val folders = response.files.filter { file ->
                file.isdir && file.isNotHidden
            } as List<RemoteItem>

            val pictures = response.files.filter { file ->
                file.isPicture && file.isNotHidden
            } as List<RemoteItem>

             FolderContent(folders, pictures)
        }
    }*/

    override suspend fun loadChildFolders(folderName: String): FolderContent {
        return loadFolder(folderName)
    }

    override suspend fun loadCurrentFolder(): FolderContent {
        return loadFolder()
    }

    override suspend fun loadParentFolder(): FolderContent {
        pathTree.removeLast()
        return loadFolder()
    }

    private suspend fun loadFolder(folderName: String? = null): FolderContent {
        folderName?.run { pathTree.add(this) }

        val stringPathBuilder = StringBuilder()
        pathTree.forEach { path ->
            stringPathBuilder.append("$path/")
        }
        val folderPath = stringPathBuilder.dropLast(1).toString()

        val response = withContext(Dispatchers.IO) {
            galleryContainer.network.getFoldersContent(folderPath)
        }

        @Suppress("UNCHECKED_CAST")
        return withContext(Dispatchers.Default) {
            val folders = response.files.filter { file ->
                file.isdir && file.isNotHidden
            } as List<RemoteItem>

            val pictures = response.files.filter { file ->
                file.isPicture && file.isNotHidden
            } as List<RemoteItem>

            FolderContent(folders, pictures, folderPath)
        }
    }
}