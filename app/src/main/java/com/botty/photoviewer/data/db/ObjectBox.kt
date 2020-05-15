package com.botty.photoviewer.data.db

import android.content.Context
import androidx.lifecycle.LiveData
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.Gallery_
import com.botty.photoviewer.data.MyObjectBox
import com.botty.photoviewer.data.connectionContainers.ConnectionParams
import com.botty.photoviewer.data.connectionContainers.ConnectionParams_
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

    override fun saveGallery(gallery: Gallery) {
        galleryBox.put(gallery)
    }

    override fun removeGallery(gallery: Gallery) {
        galleryBox.remove(gallery)
    }

    override val galleries: LiveData<List<Gallery>>
        get() = ObjectBoxLiveData(galleryBox.query { order(Gallery_.name) })

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