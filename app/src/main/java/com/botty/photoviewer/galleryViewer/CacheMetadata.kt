package com.botty.photoviewer.galleryViewer

import android.util.LruCache
import com.botty.photoviewer.data.PictureMetaContainer
import com.botty.photoviewer.data.fileStructure.MediaFile

class CacheMetadata(size: Int, private val pictures: List<MediaFile>): LruCache<Long, PictureMetaContainer>(size) {
    override fun create(key: Long?): PictureMetaContainer {
        pictures.find { picMediaFile ->
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