package com.botty.photoviewer.data

import android.os.Parcel
import android.os.Parcelable
import java.io.File

data class PictureContainer(val name: String, val hashCode: Int, var file: File? = null) :
    Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString()?.run {
            File(this)
        }
    )
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeInt(hashCode)
        parcel.writeString(file?.absolutePath)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PictureContainer> {
        const val ROTATE_90 = 6
        const val ROTATE_180 = 3
        const val ROTATE_270 = 8

        override fun createFromParcel(parcel: Parcel): PictureContainer {
            return PictureContainer(parcel)
        }

        override fun newArray(size: Int): Array<PictureContainer?> {
            return arrayOfNulls(size)
        }
    }
}