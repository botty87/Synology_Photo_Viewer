package com.botty.photoviewer.galleryViewer

import com.chibatching.kotpref.KotprefModel

object GalleryPreferences: KotprefModel() {
    var showSubfolders by booleanPref(false)
}