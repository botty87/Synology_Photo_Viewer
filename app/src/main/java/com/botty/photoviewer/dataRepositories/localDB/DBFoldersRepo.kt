package com.botty.photoviewer.dataRepositories.localDB

import com.botty.photoviewer.data.fileStructure.MediaFolder

interface DBFoldersRepo {
    fun removeGalleryFolders(galleryId: Long)
    fun saveFolder(folder: MediaFolder)
    fun removeAllFolders()
}