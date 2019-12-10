package com.botty.photoviewer.addGallery

import androidx.lifecycle.ViewModel
import com.botty.photoviewer.data.*
import io.objectbox.android.ObjectBoxLiveData
import io.objectbox.kotlin.query

class AddShareViewModel : ViewModel() {
    val connectionsLiveData by lazy {
        val query = ObjectBox.connectionParamsBox.query { order(ConnectionParams_.user) }
        ObjectBoxLiveData<ConnectionParams>(query)
    }
}