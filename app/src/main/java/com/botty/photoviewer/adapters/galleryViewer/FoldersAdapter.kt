package com.botty.photoviewer.adapters.galleryViewer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.GenericHolder
import com.botty.photoviewer.data.fileStructure.MediaFolder
import com.botty.photoviewer.tools.isNotNull
import com.botty.photoviewer.tools.network.responses.containers.Share
import kotlinx.android.synthetic.main.gallery_folder_item.view.*
import kotlinx.android.synthetic.main.gallery_parent_folder.view.*

class FoldersAdapter: RecyclerView.Adapter<GenericHolder>() {
    companion object {
        private const val FOLDER_TYPE = 1
        private const val BACK_TYPE = 2
    }

    var folders = emptyList<MediaFolder>()
    private set
    var parentName: String? = null
    private set

    override fun getItemCount(): Int = if(parentName.isNotNull()) {
        folders.size + 1
    } else {
        folders.size
    }

    override fun getItemViewType(position: Int) = if(parentName.isNotNull() && position == 0) {
        BACK_TYPE
    } else {
        FOLDER_TYPE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericHolder {
        val resLayout = if(viewType == FOLDER_TYPE) R.layout.gallery_folder_item else R.layout.gallery_parent_folder
        return LayoutInflater.from(parent.context).inflate(resLayout, parent, false)
            .run { GenericHolder(this) }
    }

    override fun onBindViewHolder(holder: GenericHolder, position: Int) {
        when(holder.itemViewType) {
            FOLDER_TYPE -> {
                val holderPos = if(parentName.isNotNull()) position -1 else position
                holder.itemView.run {
                    textViewFolderName.text = folders[holderPos].name
                }
            }

            BACK_TYPE -> {
                holder.itemView.run {
                    textViewParentName.text = parentName
                }
            }
        }
    }

    fun setFolders(folders: List<MediaFolder>, parentName: String? = null) {
        this.folders = folders
        this.parentName = parentName
        notifyDataSetChanged()
    }
}