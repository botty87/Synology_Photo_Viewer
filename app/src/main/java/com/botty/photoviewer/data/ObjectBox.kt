package com.botty.photoviewer.data

import android.content.Context
import com.botty.photoviewer.data.fileStructure.MediaFile
import com.botty.photoviewer.data.fileStructure.MediaFolder
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor

object ObjectBox {
    lateinit var boxStore: BoxStore
        private set

    fun init(context: Context) {
        boxStore = MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()
    }

    val galleryBox: Box<Gallery>
        get() = boxStore.boxFor()

    val mediaFolderBox: Box<MediaFolder>
        get() = boxStore.boxFor()

    val mediaFileBox: Box<MediaFile>
        get() = boxStore.boxFor()

    val connectionParamsBox: Box<ConnectionParams>
        get() = boxStore.boxFor(ConnectionParams::class.java)
}