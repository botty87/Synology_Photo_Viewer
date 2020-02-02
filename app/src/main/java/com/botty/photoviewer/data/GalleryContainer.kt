package com.botty.photoviewer.data

import com.botty.photoviewer.data.fileStructure.MediaFile

data class GalleryContainer(
    val name: String,
    val path: String,
    val pictures: List<MediaFile>
)