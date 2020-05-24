package com.botty.photoviewer.data

data class SyncStatus(val active: Boolean, val completedGallery: Int = 0, val totalGalleries: Int = 0, val errorMessage: String? = null) {
    constructor(completedGallery: Int, totalGalleries: Int) : this(true, completedGallery, totalGalleries)
}