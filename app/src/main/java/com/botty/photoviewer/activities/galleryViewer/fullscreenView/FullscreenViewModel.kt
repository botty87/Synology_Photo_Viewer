package com.botty.photoviewer.activities.galleryViewer.fullscreenView

import androidx.lifecycle.ViewModel
import com.botty.photoviewer.activities.galleryViewer.loader.GalleryContainer
import com.botty.photoviewer.data.remoteFolder.PictureContainer
import com.botty.photoviewer.dataRepositories.Settings

class FullscreenViewModel(private val galleryContainer: GalleryContainer, private val settings: Settings) : ViewModel() {
    val pictures: List<PictureContainer>
        get() = galleryContainer.pictures.value!!

    val presentationTimeout: Long
        get() = settings.presentationTimeout * 1000L

    val showPicInfoFullScreen: Boolean
        get() = settings.showPicInfoFullScreen
}