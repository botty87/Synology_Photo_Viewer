package com.botty.photoviewer.tools.workers.scanGalleries

import com.chibatching.kotpref.KotprefModel

object ScanGalleriesPref : KotprefModel() {
    var isGalleryOpened by booleanPref(false)
    var isFirstSyncNeeded by booleanPref(true)
}