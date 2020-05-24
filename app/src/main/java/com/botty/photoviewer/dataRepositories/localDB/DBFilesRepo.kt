package com.botty.photoviewer.dataRepositories.localDB

interface DBFilesRepo {
    fun removeGalleryFiles(galleryId: Long)
    fun removeAllFiles()
}