package com.botty.photoviewer.data

import android.os.Parcelable
import com.botty.photoviewer.components.DateParceler
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler
import java.io.File
import java.util.*

@Parcelize
@TypeParceler<Date, DateParceler>
data class PictureMetaContainer(val originDate: Date, val rotation: Int) : Parcelable {
    companion object {
        fun readFromFile(picFile: File?): PictureMetaContainer {
            picFile?.run {
                val metadata = ImageMetadataReader.readMetadata(picFile)
                val directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
                val rotation = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
                    .getInt(ExifIFD0Directory.TAG_ORIENTATION)
                return PictureMetaContainer(directory.dateOriginal, rotation)
            } ?: throw NoMetadataException()
        }
    }

    class NoMetadataException: Exception()

    @Parcelize
    data class ParcelablePair(val hash: Int, val pictureMetaContainer: PictureMetaContainer) : Parcelable
}