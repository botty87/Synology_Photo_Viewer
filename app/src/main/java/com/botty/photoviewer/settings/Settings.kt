package com.botty.photoviewer.settings

import com.chibatching.kotpref.KotprefModel

object Settings : KotprefModel() {
    var autoUpdateGallery by booleanPref(false)
    var showSubFoldersPic by booleanPref(false)
}