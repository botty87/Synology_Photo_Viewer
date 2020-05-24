package com.botty.photoviewer.adapters.galleryViewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.botty.photoviewer.R
import com.botty.photoviewer.activities.galleryViewer.CacheMetadata
import com.botty.photoviewer.adapters.GenericHolder
import com.botty.photoviewer.components.Tools
import com.botty.photoviewer.components.clear
import com.botty.photoviewer.components.glide.GlideTools
import com.botty.photoviewer.components.showErrorToast
import com.botty.photoviewer.data.remoteFolder.PictureContainer
import com.bumptech.glide.RequestManager
import kotlinx.android.synthetic.main.picture_item.view.*
import org.koin.core.KoinComponent
import org.koin.core.get


class PicturesAdapter(private val pictureMetaCache: CacheMetadata,
                      private val context: Context) : RecyclerView.Adapter<GenericHolder>(), KoinComponent {

    private val glideManager: RequestManager = get()

    private var picturesList: List<PictureContainer>? = null

    private val dateParser = Tools.standardDateParser

    override fun getItemCount() = picturesList?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericHolder {
        return LayoutInflater.from(parent.context).inflate(R.layout.picture_item, parent, false)
            .run { GenericHolder(this) }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: GenericHolder, position: Int) {
        val picture = picturesList!![holder.adapterPosition]
        when {
            picture.file?.exists() == true -> {
                val metaCacheId = picture.hashCode
                if(picture.name.endsWith(".webp", true)) {
                    runCatching {
                        pictureMetaCache[metaCacheId].rotation
                    }.onSuccess { rotation ->
                        GlideTools.loadWebpImageIntoView(glideManager, holder.itemView.imageViewPicture, picture, context, rotation)
                    }.onFailure {
                        GlideTools.loadImageIntoView(glideManager, holder.itemView.imageViewPicture, picture, context)
                    }
                } else {
                    GlideTools.loadImageIntoView(glideManager, holder.itemView.imageViewPicture, picture, context)
                }

                runCatching {
                    pictureMetaCache[metaCacheId].originDate
                }.onSuccess { picDate ->
                    holder.itemView.textViewDate.text = dateParser.format(picDate)
                }
            }

            //TODO review!
            picture.timeoutException -> {
                GlideTools.setErrorImage(glideManager, holder.itemView.imageViewPicture)
                context.showErrorToast(R.string.timeoutException)
            }

            else -> {
                CircularProgressDrawable(context).apply {
                    strokeWidth = 5f
                    centerRadius = 30f
                    this.setColorSchemeColors(Color.RED)
                    start()
                }.let {prog ->
                    holder.itemView.imageViewPicture.setImageDrawable(prog)
                }
                picturesList!![holder.adapterPosition].file = null
            }
        }

        holder.itemView.textViewName.text = picture.name
    }

    override fun getItemId(position: Int): Long = picturesList!![position].hashCode.toLong()

    override fun onViewRecycled(holder: GenericHolder) {
        glideManager.clear(holder.itemView.imageViewPicture)
        holder.itemView.textViewDate.clear()
        super.onViewRecycled(holder)
    }

    fun setNewPictures(pictures: List<PictureContainer>) {
        picturesList = pictures
        notifyDataSetChanged()
    }

}