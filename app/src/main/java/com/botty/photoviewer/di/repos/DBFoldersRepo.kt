package com.botty.photoviewer.di.repos

import com.botty.photoviewer.data.fileStructure.MediaFolder

interface DBFoldersRepo {
    fun removeGalleryFolders(galleryId: Long)
    fun saveFolder(folder: MediaFolder)
    fun removeAllFolders()
}