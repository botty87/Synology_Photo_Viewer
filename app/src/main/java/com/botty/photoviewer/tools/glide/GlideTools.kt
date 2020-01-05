package com.botty.photoviewer.tools.glide

import android.content.Context
import android.graphics.Color
import android.widget.ImageView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.botty.photoviewer.R
import com.botty.photoviewer.data.PictureContainer
import com.botty.photoviewer.data.fileStructure.MediaFile
import com.bumptech.glide.RequestManager

object GlideTools {

    private fun getPlaceHolder(context: Context): CircularProgressDrawable {
        return CircularProgressDrawable(context).apply {
            strokeWidth = 5f
            centerRadius = 30f
            this.setColorSchemeColors(Color.RED)
            start()
        }
    }

    fun loadImageIntoView(glide: RequestManager,
                          imageView: ImageView,
                          mediaFile: MediaFile,
                          context: Context) {
        loadImage(glide, imageView, mediaFile, context)
    }

    fun loadWebpImageIntoView(glide: RequestManager,
                              imageView: ImageView,
                              mediaFile: MediaFile,
                              context: Context,
                              rotation: Int) {

        when(rotation) {
            PictureContainer.ROTATE_90 -> {
                loadImage(glide, imageView, mediaFile, context, RotateTransformation(90f))
            }
            PictureContainer.ROTATE_180 -> {
                loadImage(glide, imageView, mediaFile, context, RotateTransformation(180f))
            }
            PictureContainer.ROTATE_270 -> {
                loadImage(glide, imageView, mediaFile, context, RotateTransformation(270f))
            }
            else -> {
                loadImage(glide, imageView, mediaFile, context)
            }
        }
    }

    fun setErrorImage(glide: RequestManager,
                      imageView: ImageView) {
        glide
            .load(R.drawable.ic_broken_image_220dp)
            .into(imageView)
    }

    private fun loadImage(glide: RequestManager,
                  imageView: ImageView,
                  pictureContainer: PictureContainer,
                  context: Context,
                  rotateTransformation: RotateTransformation? = null) {
        glide
            .load(pictureContainer.file!!)
            .placeholder(getPlaceHolder(context))
            .error(R.drawable.ic_broken_image_220dp)
            .thumbnail(0.3f)
            .apply {
                rotateTransformation?.run { transform(this) }
            }
            .into(imageView)
    }

    private fun loadImage(glide: RequestManager,
                          imageView: ImageView,
                          mediaFile: MediaFile,
                          context: Context,
                          rotateTransformation: RotateTransformation? = null) {
        glide
            .load(mediaFile.file!!)
            .placeholder(getPlaceHolder(context))
            .error(R.drawable.ic_broken_image_220dp)
            .thumbnail(0.3f)
            .apply {
                rotateTransformation?.run { transform(this) }
            }
            .into(imageView)
    }
}