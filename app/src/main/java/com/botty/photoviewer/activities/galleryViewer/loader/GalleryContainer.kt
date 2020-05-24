package com.botty.photoviewer.activities.galleryViewer.loader

import androidx.lifecycle.MutableLiveData
import com.botty.photoviewer.data.connectionContainers.SessionParams
import com.botty.photoviewer.data.remoteFolder.PictureContainer
import com.botty.photoviewer.dataRepositories.remote.Network
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.parameter.parametersOf

//val gallery: Gallery,

//Because the GalleryViewActivity has 2 viewmodels (because of its complexity) this class helps to share the common object between them
class GalleryContainer (val pictures:MutableLiveData<List<PictureContainer>> = MutableLiveData<List<PictureContainer>>()): KoinComponent {
    lateinit var currentGalleryPath: String
    lateinit var network: Network

    constructor(pictures: List<PictureContainer>, currentGalleryPath: String, sessionParams: SessionParams) : this(MutableLiveData(pictures)) {
        this.currentGalleryPath = currentGalleryPath
        this.network = get{ parametersOf(sessionParams) }
    }
}