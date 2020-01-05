package com.botty.photoviewer.galleryViewer

import android.util.LruCache
import com.botty.photoviewer.data.PictureContainer
import com.botty.photoviewer.data.PictureMetaContainer

class OldCacheMetadata(size: Int, private val pictures: List<PictureContainer>): LruCache<Int, PictureMetaContainer>(size) {
    override fun create(key: Int?): PictureMetaContainer {
        pictures.find { picContainer ->
            picContainer.hashCode == key
        }?.file.run {
            try {
                return PictureMetaContainer.readFromFile(this)
            } catch (e: Exception) {
                throw PictureMetaContainer.NoMetadataException()
            }
        }
    }
}