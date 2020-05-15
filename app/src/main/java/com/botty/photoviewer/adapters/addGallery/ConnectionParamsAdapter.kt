package com.botty.photoviewer.adapters.addGallery

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.GenericHolder
import com.botty.photoviewer.components.get
import com.botty.photoviewer.components.size
import com.botty.photoviewer.data.connectionContainers.ConnectionParams
import kotlinx.android.synthetic.main.connection_item.view.*

class ConnectionParamsAdapter : RecyclerView.Adapter<GenericHolder>() {

    private val diffUtilCallback = object: DiffUtil.ItemCallback<ConnectionParams>() {
        override fun areItemsTheSame(oldItem: ConnectionParams, newItem: ConnectionParams): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ConnectionParams, newItem: ConnectionParams): Boolean {
            return oldItem == newItem
        }
    }
    private val connections : AsyncListDiffer<ConnectionParams> = AsyncListDiffer(this, diffUtilCallback)
    var onConnectionClick: ((ConnectionParams) -> Unit)? = null

    var isEnabled = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LayoutInflater.from(parent.context).inflate(R.layout.connection_item, parent, false)
            .run { GenericHolder(this) }

    override fun getItemCount() = connections.size

    override fun onBindViewHolder(holder: GenericHolder, position: Int) {
        holder.itemView.textViewPhotoName.text = connections
            .currentList[position].run { "$user - $address" }
        if(isEnabled) {
            holder.itemView.isEnabled = true
            holder.itemView.setOnFocusChangeListener { view, focused ->
                if (focused) {
                    view.background = ColorDrawable(Color.GRAY)
                } else {
                    view.background = ColorDrawable(Color.TRANSPARENT)
                }
            }
            holder.itemView.setOnClickListener {
                onConnectionClick?.invoke(connections[position])
            }
        } else {
            holder.itemView.isEnabled = false
            holder.itemView.onFocusChangeListener = null
            holder.itemView.setOnClickListener(null)
            holder.itemView.background = ColorDrawable(Color.TRANSPARENT)
        }
    }

    fun setConnections(connections: List<ConnectionParams>) {
        this.connections.submitList(connections)
    }
}