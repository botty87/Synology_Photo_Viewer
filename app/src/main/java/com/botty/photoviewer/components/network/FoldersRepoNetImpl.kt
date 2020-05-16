package com.botty.photoviewer.components.network

import com.botty.photoviewer.activities.galleryViewer.loader.GalleryContainer
import com.botty.photoviewer.data.remoteFolder.FolderContent
import com.botty.photoviewer.data.remoteFolder.RemoteItem
import com.botty.photoviewer.di.repos.FoldersRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FoldersRepoNetImpl(private val galleryContainer: GalleryContainer): FoldersRepo {
    override suspend fun loadFolderContent(path: String): FolderContent {

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
    }
}