package com.botty.photoviewer.adapters.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.GenericHolder
import com.botty.photoviewer.data.Gallery_
import com.botty.photoviewer.data.ObjectBox
import io.objectbox.kotlin.query
import kotlinx.android.synthetic.main.gallery_folder_item.view.*

class GalleriesAdapter: RecyclerView.Adapter<GenericHolder>() {

    private val galleries = ObjectBox.galleryBox.query { order(Gallery_.name) }.find()

    override fun getItemCount() = galleries.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericHolder {
        return LayoutInflater.from(parent.context).inflate(R.layout.gallery_folder_item, parent, false)
            .run { GenericHolder(this) }
    }

    override fun onBindViewHolder(holder: GenericHolder, position: Int) {
        holder.itemView.textViewFolderName.text = galleries[position].name
    }

    fun removeGallery(position: Int) {
        galleries.removeAt(position).run {
            ObjectBox.galleryBox.remove(this)
        }
        notifyDataSetChanged()
    }
}