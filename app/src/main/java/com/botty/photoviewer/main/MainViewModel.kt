package com.botty.photoviewer.main

import androidx.lifecycle.ViewModel
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.Gallery_
import com.botty.photoviewer.data.ObjectBox
import io.objectbox.android.ObjectBoxLiveData
import io.objectbox.kotlin.query


class MainViewModel : ViewModel() {
    val galleriesLiveData by lazy {
        val query = ObjectBox.galleryBox.query { order(Gallery_.name) }
        ObjectBoxLiveData(query)
    }
}