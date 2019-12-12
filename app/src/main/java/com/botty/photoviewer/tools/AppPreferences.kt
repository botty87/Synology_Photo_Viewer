package com.botty.photoviewer.tools

import com.chibatching.kotpref.KotprefModel

object AppPreferences: KotprefModel() {
    var showPictureInfo by booleanPref(default = true)
}