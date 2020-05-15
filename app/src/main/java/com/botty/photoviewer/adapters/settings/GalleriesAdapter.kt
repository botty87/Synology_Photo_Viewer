package com.botty.photoviewer.adapters.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.observe
import androidx.recyclerview.widget.RecyclerView
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.GenericHolder
import com.botty.photoviewer.data.Gallery
import kotlinx.android.synthetic.main.gallery_folder_item.view.*

class GalleriesAdapter(galleriesLiveData: LiveData<List<Gallery>>, lifecycleOwner: LifecycleOwner): RecyclerView.Adapter<GenericHolder>() {

    private var galleries: List<Gallery> = emptyList()

    init {
        galleriesLiveData.observe(lifecycleOwner) { newGalleries ->
            galleries = newGalleries
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = galleries.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericHolder {
        return LayoutInflater.from(parent.context).inflate(R.layout.gallery_folder_item, parent, false)
            .run { GenericHolder(this) }
    }

    override fun onBindViewHolder(holder: GenericHolder, position: Int) {
        holder.itemView.textViewFolderName.text = galleries[position].name
    }
}