package com.botty.photoviewer.galleryViewer.fullscreenView

import androidx.lifecycle.ViewModel
import com.botty.photoviewer.data.PictureContainer
import com.botty.photoviewer.data.Settings
import com.botty.photoviewer.galleryViewer.loader.GalleryContainer

class FullscreenViewModel(private val galleryContainer: GalleryContainer, private val settings: Settings) : ViewModel() {
    val pictures: List<PictureContainer>
        get() = galleryContainer.pictures.value!!

    val presentationTimeout: Long
        get() = settings.presentationTimeout * 1000L

    val showPicInfoFullScreen: Boolean
        get() = settings.showPicInfoFullScreen
}