package com.botty.photoviewer.adapters.galleryViewer.pictureAdapter

import android.widget.TextView
import com.botty.photoviewer.R
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item

class HeaderItem(private val title: String, private val headerSpanCount: Int) : Item() {

    override fun bind(holder: GroupieViewHolder, position: Int) {
        (holder.containerView as TextView).text = title
        holder.containerView.isFocusable = false
    }

    override fun getLayout() = R.layout.header_album_item

    override fun getSpanSize(spanCount: Int, position: Int): Int {
        val test = headerSpanCount
        return headerSpanCount
    }
}