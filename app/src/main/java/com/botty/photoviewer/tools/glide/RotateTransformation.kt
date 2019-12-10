package com.botty.photoviewer.tools.glide

import android.graphics.Bitmap
import android.graphics.Matrix
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation

import java.security.MessageDigest

class RotateTransformation(private val rotationAngle: Float): BitmapTransformation() {

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        return Matrix().run {
            postRotate(rotationAngle)
            Bitmap.createBitmap(toTransform, 0, 0, toTransform.width, toTransform.height, this, true)
        }
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("rotate$rotationAngle".toByteArray())
    }
}