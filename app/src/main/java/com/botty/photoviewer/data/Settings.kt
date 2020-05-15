package com.botty.photoviewer.data

import com.chibatching.kotpref.KotprefModel

class Settings : KotprefModel() {
    var showPicInfoFullScreen by booleanPref(default = true)
    var presentationTimeout by intPref(default = 5)
}