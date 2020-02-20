package com.botty.photoviewer.adapters.galleryViewer.pictureAdapter

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.botty.photoviewer.R
import com.botty.photoviewer.data.fileStructure.MediaFile
import com.botty.photoviewer.galleryViewer.CacheMetadata
import com.botty.photoviewer.tools.Tools
import com.botty.photoviewer.tools.clear
import com.botty.photoviewer.tools.glide.GlideTools
import com.botty.photoviewer.tools.showErrorToast
import com.bumptech.glide.RequestManager
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.picture_item.view.*

class PictureItem(private val picture: MediaFile, private val resLoader: ResLoader): Item() {

    override fun getLayout() = R.layout.picture_item

    override fun bind(holder: GroupieViewHolder, position: Int) {
        resLoader.run {
            when {
                picture.file?.exists() == true -> {
                    if (picture.name.endsWith(".webp", true)) {
                        runCatching {
                            pictureMetaCache[picture.id].rotation
                        }.onSuccess { rotation ->
                            GlideTools.loadWebpImageIntoView(
                                glideManager,
                                holder.itemView.imageViewPicture,
                                picture,
                                context,
                                rotation
                            )
                        }.onFailure {
                            GlideTools.loadImageIntoView(
                                glideManager,
                                holder.itemView.imageViewPicture,
                                picture,
                                context
                            )
                        }
                    } else {
                        GlideTools.loadImageIntoView(
                            glideManager,
                            holder.itemView.imageViewPicture,
                            picture,
                            context
                        )
                    }

                    runCatching {
                        pictureMetaCache[picture.id].originDate
                    }.onSuccess { picDate ->
                        holder.itemView.textViewDate.text = Tools.standardDateParser.format(picDate)
                    }
                }

                picture.executionException -> {
                    GlideTools.setErrorImage(glideManager, holder.itemView.imageViewPicture)
                    context.showErrorToast(R.string.timeout_or_missing_exception)
                }

                else -> {
                    CircularProgressDrawable(context).apply {
                        strokeWidth = 5f
                        centerRadius = 30f
                        this.setColorSchemeColors(Color.RED)
                        start()
                    }.let { prog ->
                        holder.itemView.imageViewPicture.setImageDrawable(prog)
                    }
                    picture.file = null
                }
            }
            holder.itemView.textViewName.text = picture.name
        }
    }

    override fun unbind(holder: GroupieViewHolder) {
        super.unbind(holder)
        resLoader.glideManager.clear(holder.itemView.imageViewPicture)
        holder.itemView.textViewDate.clear()

    }

    data class ResLoader(
        val glideManager: RequestManager,
        val pictureMetaCache: CacheMetadata,
        val context: Context
    )
}