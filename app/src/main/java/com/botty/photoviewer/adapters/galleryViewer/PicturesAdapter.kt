package com.botty.photoviewer.adapters.galleryViewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.GenericHolder
import com.botty.photoviewer.data.fileStructure.MediaFile
import com.botty.photoviewer.galleryViewer.CacheMetadata
import com.botty.photoviewer.tools.Tools
import com.botty.photoviewer.tools.clear
import com.botty.photoviewer.tools.glide.GlideTools
import com.botty.photoviewer.tools.showErrorToast
import com.bumptech.glide.RequestManager
import kotlinx.android.synthetic.main.picture_item.view.*
import java.lang.IndexOutOfBoundsException

private const val PICTURE_TYPE = 1
private const val HEADER_TYPE = 2

class PicturesAdapter(private val glideManager: RequestManager,
                      private val pictureMetaCache: CacheMetadata,
                      private val picturesList: List<MediaFile>,
                      private val context: Context) : RecyclerView.Adapter<GenericHolder>() {

    private val dateParser = Tools.standardDateParser

    override fun getItemCount() = picturesList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericHolder {
        val layout = if(viewType == PICTURE_TYPE) R.layout.picture_item
            else R.layout.header_album_item

        return LayoutInflater.from(parent.context).inflate(layout, parent, false)
            .run { GenericHolder(this) }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: GenericHolder, position: Int) {
        val picture = picturesList[holder.adapterPosition]
        if(picture.isPicture()) {
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
                        holder.itemView.textViewDate.text = dateParser.format(picDate)
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
                    picturesList[holder.adapterPosition].file = null
                }
            }

            holder.itemView.textViewName.text = picture.name
        } else {
            (holder.itemView as TextView).text = picture.name
        }
    }

    override fun getItemId(position: Int): Long = picturesList[position].id

    override fun getItemViewType(position: Int): Int {
        return try {
            if (picturesList[position].isPicture()) {
                PICTURE_TYPE
            } else {
                HEADER_TYPE
            }
        } catch (e: IndexOutOfBoundsException) {
            PICTURE_TYPE
        }
    }

    override fun onViewRecycled(holder: GenericHolder) {
        if(getItemViewType(holder.adapterPosition) == PICTURE_TYPE) {
            glideManager.clear(holder.itemView.imageViewPicture)
            holder.itemView.textViewDate.clear()
        }
        super.onViewRecycled(holder)
    }
}