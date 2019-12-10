package com.botty.photoviewer.data

import android.os.Parcelable
import com.botty.photoviewer.tools.network.Network
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SessionParams(val address: String,
                         val sid: String,
                         val port: Int,
                         val https: Boolean,
                         val connectionId: Long = 0L) : Parcelable {

    fun getPicFullPath(pictureGalleryPath: String, picName: String): String {
        val fullPicPath = "$pictureGalleryPath/$picName"
        return Network.getPictureUrl(this, fullPicPath)
    }
}