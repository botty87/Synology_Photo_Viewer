package com.botty.photoviewer.dataRepositories.localDB.impl

import android.content.Context
import androidx.lifecycle.LiveData
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.Gallery_
import com.botty.photoviewer.data.MyObjectBox
import com.botty.photoviewer.data.connectionContainers.ConnectionParams
import com.botty.photoviewer.data.connectionContainers.ConnectionParams_
import com.botty.photoviewer.data.fileStructure.MediaFile
import com.botty.photoviewer.data.fileStructure.MediaFile_
import com.botty.photoviewer.data.fileStructure.MediaFolder
import com.botty.photoviewer.data.fileStructure.MediaFolder_
import com.botty.photoviewer.dataRepositories.localDB.AppDB
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.android.ObjectBoxLiveData
import io.objectbox.kotlin.boxFor
import io.objectbox.kotlin.query

class ObjectBox: AppDB {
    lateinit var boxStore: BoxStore
        private set

    fun init(context: Context) {
        boxStore = MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()
    }

    private val galleryBox: Box<Gallery>
        get() = boxStore.boxFor()

    private val connectionParamsBox: Box<ConnectionParams>
        get() = boxStore.boxFor()

    private val mediaFolderBox: Box<MediaFolder>
        get() = boxStore.boxFor()

    private val mediaFileBox: Box<MediaFile>
        get() = boxStore.boxFor()

    override fun saveGallery(gallery: Gallery) {
        galleryBox.put(gallery)
    }

    override fun saveGalleries(galleries: List<Gallery>) {
        galleryBox.put(galleries)
    }

    override fun removeGallery(gallery: Gallery) {
        galleryBox.remove(gallery)
    }

    override fun removeGalleryFolders(galleryId: Long) {
        mediaFolderBox.query {
            equal(MediaFolder_.galleryId, galleryId)
        }.remove()
    }

    override fun saveFolder(folder: MediaFolder) {
        mediaFolderBox.put(folder)
    }

    override fun removeAllFolders() {
        mediaFolderBox.removeAll()
    }

    override fun removeGalleryFiles(galleryId: Long) {
        mediaFileBox.query {
            equal(MediaFile_.galleryId, galleryId)
        }.remove()
    }

    override fun removeAllFiles() {
        mediaFileBox.removeAll()
    }

    override val galleriesLiveData: LiveData<List<Gallery>>
        get() = ObjectBoxLiveData(galleryBox.query { order(Gallery_.name) })

    override val galleries: List<Gallery>
        get() = galleryBox.all

    override val galleriesWithNoSync: List<Gallery>
        get() = galleryBox.query { isNull(Gallery_.lastSync) }.find()

    override val hasGalleryToSync: Boolean
        get() = galleriesWithNoSync.isNotEmpty()

    override fun getGallery(galleryId: Long): Gallery = galleryBox[galleryId]

    override val connections: LiveData<List<ConnectionParams>>
        get() = ObjectBoxLiveData(connectionParamsBox.query { order(ConnectionParams_.user) })

    override fun checkIfConnectionExist(conParams: ConnectionParams): Boolean {
        return connectionParamsBox.query {
            equal(ConnectionParams_.address, conParams.address)
            equal(ConnectionParams_.user, conParams.user)
        }.findFirst()?.run { true } ?: false
    }
}