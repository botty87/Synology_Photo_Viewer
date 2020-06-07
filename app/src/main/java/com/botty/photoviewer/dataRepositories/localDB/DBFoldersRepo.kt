package com.botty.photoviewer.dataRepositories.localDB

import com.botty.photoviewer.data.fileStructure.MediaFile
import com.botty.photoviewer.data.fileStructure.MediaFolder

interface DBFoldersRepo {
    fun removeGalleryFolders(galleryId: Long)
    fun saveFolder(folder: MediaFolder)
    fun removeAllFolders()
    fun getFolder(folderId: Long): MediaFolder
    suspend fun updateFolder(folder: MediaFolder, foldersToAdd: List<MediaFolder>, foldersToRemove: List<MediaFolder>,
        filesToAdd: List<MediaFile>, filesToRemove: List<MediaFile>)
}