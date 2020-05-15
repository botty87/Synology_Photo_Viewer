package com.botty.photoviewer.adapters.addGallery

import android.view.LayoutInflater
import android.view.ViewGroup
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.AdapterWithFocus
import com.botty.photoviewer.adapters.GenericHolder
import com.botty.photoviewer.components.network.responses.containers.Share
import kotlinx.android.synthetic.main.folder_item.view.*

class FoldersAdapter : AdapterWithFocus<GenericHolder>() {
    companion object {
        private const val FOLDER_TYPE = 1
        private const val BACK_TYPE = 2
    }

    private var isBackEnabled = false
    private var folders: List<Share> = emptyList()

    var onCallLogClick: ((Share) -> Unit)? = null
    var onBackClick: (() -> Unit)? = null

    override fun getItemCount(): Int = if(isBackEnabled) {
        folders.size + 1
    } else {
        folders.size
    }

    override fun getItemViewType(position: Int) = if(isBackEnabled && position == 0) {
        BACK_TYPE
    } else {
        FOLDER_TYPE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericHolder {
        val resLayout = if(viewType == FOLDER_TYPE) R.layout.folder_item else R.layout.folder_back_item
        return LayoutInflater.from(parent.context).inflate(resLayout, parent, false)
            .run { GenericHolder(this) }
    }

    override fun onBindViewHolder(holder: GenericHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        when(holder.itemViewType) {
            FOLDER_TYPE -> {
                val holderPos = if(isBackEnabled) position - 1 else position
                holder.itemView.textViewPhotoName.text = folders[holderPos].name
                holder.itemView.setOnClickListener {
                    onCallLogClick?.invoke(folders[holderPos])
                }
            }

            BACK_TYPE -> {
                holder.itemView.setOnClickListener { onBackClick?.invoke() }
            }
        }
    }

    fun updateFolders(folders: List<Share>, isBackEnabled: Boolean) {
        this.folders = folders
        this.isBackEnabled = isBackEnabled
        notifyDataSetChanged()
    }
}