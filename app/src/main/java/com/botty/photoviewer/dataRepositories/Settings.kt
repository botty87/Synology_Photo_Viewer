package com.botty.photoviewer.dataRepositories

import com.chibatching.kotpref.KotprefModel

class Settings : KotprefModel() {
    var showPicInfoFullScreen by booleanPref(default = true)
    var presentationTimeout by intPref(default = 5)
    var dbMode by booleanPref(default = false)
    var dailyScan by booleanPref(default = false)
}