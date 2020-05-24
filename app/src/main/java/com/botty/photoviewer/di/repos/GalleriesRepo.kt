package com.botty.photoviewer.di.repos

import androidx.lifecycle.LiveData
import com.botty.photoviewer.data.Gallery

interface GalleriesRepo {
    val galleriesLiveData: LiveData<List<Gallery>>
    val galleries: List<Gallery>
    val galleriesWithNoSync: List<Gallery>
    val hasGalleryToSync: Boolean
    fun getGallery(galleryId: Long): Gallery
    fun saveGallery(gallery: Gallery)
    fun saveGalleries(galleries: List<Gallery>)
    fun removeGallery(gallery: Gallery)
}