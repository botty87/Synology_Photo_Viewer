package com.botty.photoviewer.settings

import com.chibatching.kotpref.KotprefModel

object Settings : KotprefModel() {
    var showPicInfoFullScreen by booleanPref(default = true)
    var presentationTimeout by intPref(default = 5)
}