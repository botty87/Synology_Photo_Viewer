package com.botty.photoviewer.activities.galleryViewer

import android.util.LruCache
import androidx.lifecycle.MutableLiveData
import com.botty.photoviewer.data.PictureMetaContainer
import com.botty.photoviewer.data.remoteFolder.PictureContainer

class CacheMetadata(size: Int, private var picturesLiveData: MutableLiveData<List<PictureContainer>>): LruCache<Int, PictureMetaContainer>(size) {
    override fun create(key: Int?): PictureMetaContainer {
        picturesLiveData.value?.let { pictures ->
            pictures.find { picContainer ->
                picContainer.hashCode == key
            }?.file.run {
                try {
                    return PictureMetaContainer.readFromFile(this)
                } catch (e: Exception) {
                    throw PictureMetaContainer.NoMetadataException()
                }
            }
        } ?: throw PictureMetaContainer.NoMetadataException()
    }
}