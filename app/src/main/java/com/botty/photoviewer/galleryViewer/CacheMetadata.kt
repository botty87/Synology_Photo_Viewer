package com.botty.photoviewer.galleryViewer

import android.util.LruCache
import com.botty.photoviewer.data.GalleryContainer
import com.botty.photoviewer.data.PictureMetaContainer
import com.botty.photoviewer.data.fileStructure.MediaFile

class CacheMetadata(size: Int, private val galleryContainers: List<GalleryContainer>): LruCache<Long, PictureMetaContainer>(size) {
    override fun create(key: Long?): PictureMetaContainer {
        galleryContainers.flatMap { galleryContainer ->
            galleryContainer.pictures
        }.find { picMediaFile ->
            picMediaFile.id == key
        }?.file.run {
            try {
                return PictureMetaContainer.readFromFile(this)
            } catch (e: Exception) {
                throw PictureMetaContainer.NoMetadataException()
            }
        }
    }
}