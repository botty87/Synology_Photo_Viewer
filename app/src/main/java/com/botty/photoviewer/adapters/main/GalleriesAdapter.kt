package com.botty.photoviewer.adapters.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.AdapterWithFocus
import com.botty.photoviewer.adapters.GenericHolder
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.tools.get
import com.botty.photoviewer.tools.size
import com.bumptech.glide.RequestManager
import kotlinx.android.synthetic.main.add_gallery_item.view.*
import kotlinx.android.synthetic.main.gallery_item.view.*

class GalleriesAdapter(private val glide: RequestManager) : AdapterWithFocus<GenericHolder>(true) {

    var onAddNewClick: (() -> Unit)? = null
    var onSettingsClick: (() -> Unit)? = null
    var onGalleryClick: ((Gallery) -> Unit)? = null

    private val diffUtilCallback = object: DiffUtil.ItemCallback<Gallery>() {
        override fun areItemsTheSame(oldItem: Gallery, newItem: Gallery): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Gallery, newItem: Gallery): Boolean {
            return oldItem == newItem
        }
    }
    private val galleries = AsyncListDiffer(this, diffUtilCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericHolder {
        val resLayout = when (viewType) {
            GALLERY_TYPE -> R.layout.gallery_item
            SETTINGS_TYPE -> R.layout.settings_item
            else -> R.layout.add_gallery_item
        }
        return LayoutInflater.from(parent.context).inflate(resLayout, parent, false)
            .run { GenericHolder(this) }
    }

    override fun getItemCount() = galleries.size + 2

    override fun onBindViewHolder(holder: GenericHolder, position: Int) {
        fun bindGalleryView() {
            galleries[position].let { gallery ->
                holder.itemView.textViewPhotoName.text = gallery.name
                glide.load(R.drawable.add_gallery_background)
                    .into(holder.itemView.imageViewGallery)

                holder.itemView.setOnClickListener {
                    onGalleryClick?.invoke(gallery)
                }
            }
        }

        fun bindAddNewView() {
            glide.load(R.drawable.add_gallery_background)
                .into(holder.itemView.imageViewBackground)
            holder.itemView.setOnClickListener {
                onAddNewClick?.invoke()
            }
        }

        fun bindSettingsView() {
            holder.itemView.setOnClickListener {
                onSettingsClick?.invoke()
            }
        }

        super.onBindViewHolder(holder, position)
        when(holder.itemViewType) {
            GALLERY_TYPE -> bindGalleryView()
            ADD_NEW_TYPE -> bindAddNewView()
            SETTINGS_TYPE -> bindSettingsView()
        }
    }

    override fun getItemViewType(position: Int) =
        when {
            position < galleries.size -> {
                GALLERY_TYPE
            }
            position == galleries.size -> {
                ADD_NEW_TYPE
            }
            else -> {
                SETTINGS_TYPE
            }
        }

    fun setNewGalleries(galleries: List<Gallery>) {
        this.galleries.submitList(galleries)
    }

    companion object {
        private const val GALLERY_TYPE = 1
        private const val ADD_NEW_TYPE = 2
        private const val SETTINGS_TYPE = 3
    }
}