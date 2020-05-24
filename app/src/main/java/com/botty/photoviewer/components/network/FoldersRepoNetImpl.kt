package com.botty.photoviewer.components.network

import com.botty.photoviewer.activities.galleryViewer.loader.GalleryContainer
import com.botty.photoviewer.components.removeLast
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.remoteFolder.FolderContent
import com.botty.photoviewer.data.remoteFolder.RemoteItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FoldersRepoNetImpl : FoldersRepoNet {
    private var galleryContainer: GalleryContainer?
    override var pathTree: MutableList<String>
    private lateinit var network: Network

    constructor(galleryContainer: GalleryContainer, gallery: Gallery) {
        this.galleryContainer = galleryContainer
        this.pathTree = mutableListOf(gallery.path)
    }

    constructor(network: Network, gallery: Gallery) {
        this.network = network
        this.galleryContainer = null
        this.pathTree = mutableListOf(gallery.path)
    }

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
        return loadFolderPath(stringPathBuilder.dropLast(1).toString())
    }

    override suspend fun loadFolderPath(folderPath: String): FolderContent {
        val response = withContext(Dispatchers.IO) {
            val network = galleryContainer?.network ?: network
            network.getFoldersContent(folderPath)
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