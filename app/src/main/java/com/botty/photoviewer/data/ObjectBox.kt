package com.botty.photoviewer.data

import android.content.Context
import io.objectbox.Box
import io.objectbox.BoxStore

object ObjectBox {
    lateinit var boxStore: BoxStore
        private set

    fun init(context: Context) {
        boxStore = MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()
    }

    val galleryBox: Box<Gallery>
        get() = boxStore.boxFor(Gallery::class.java)

    val connectionParamsBox: Box<ConnectionParams>
        get() = boxStore.boxFor(ConnectionParams::class.java)
}