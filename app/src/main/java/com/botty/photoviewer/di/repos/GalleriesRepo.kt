package com.botty.photoviewer.di.repos

import androidx.lifecycle.LiveData
import com.botty.photoviewer.data.Gallery

interface GalleriesRepo {
    val galleries: LiveData<List<Gallery>>
    fun getGallery(galleryId: Long): Gallery
    fun saveGallery(gallery: Gallery)
    fun removeGallery(gallery: Gallery)
}