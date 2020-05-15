package com.botty.photoviewer.data.connectionContainers

import android.os.Parcel
import android.os.Parcelable

// val connectionId: Long = 0L removed

class SessionParams(address: String,
                         val sid: String,
                         port: Int,
                         https: Boolean) : BaseUrlContainer(address, port, https), Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readInt() == 1
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(address)
        parcel.writeString(sid)
        parcel.writeInt(port)
        parcel.writeInt(if(https) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SessionParams> {
        override fun createFromParcel(parcel: Parcel): SessionParams {
            return SessionParams(parcel)
        }

        override fun newArray(size: Int): Array<SessionParams?> {
            return arrayOfNulls(size)
        }
    }

}