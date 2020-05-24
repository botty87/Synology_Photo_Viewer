package com.botty.photoviewer.di.repos

interface DBFilesRepo {
    fun removeGalleryFiles(galleryId: Long)
    fun removeAllFiles()
}