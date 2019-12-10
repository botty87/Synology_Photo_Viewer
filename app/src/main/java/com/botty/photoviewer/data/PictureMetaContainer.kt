package com.botty.photoviewer.data

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import java.io.File
import java.util.Date

data class PictureMetaContainer(val originDate: Date, val rotation: Int) {

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
}