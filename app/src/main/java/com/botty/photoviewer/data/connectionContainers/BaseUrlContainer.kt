package com.botty.photoviewer.data.connectionContainers

import io.objectbox.annotation.BaseEntity

@BaseEntity
abstract class BaseUrlContainer(val address: String,
                                val port: Int,
                                val https: Boolean) {

    val baseUrl : String
        get() {
            val protocol = if(https) { "https" } else { "http" }
            return "$protocol://$address:$port/webapi/"
        }
}