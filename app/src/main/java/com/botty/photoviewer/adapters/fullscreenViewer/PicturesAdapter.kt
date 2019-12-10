package com.botty.photoviewer.adapters.fullscreenViewer

import android.content.Context
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.botty.photoviewer.R
import com.botty.photoviewer.data.PictureContainer
import com.botty.photoviewer.data.PictureMetaContainer
import com.botty.photoviewer.tools.glide.GlideTools
import com.botty.photoviewer.tools.set
import com.bumptech.glide.RequestManager
import kotlinx.android.synthetic.main.picture_fullscreen_item.view.imageViewPicture

class PicturesAdapter(
    private val pictures: List<PictureContainer>,
    private val glide: RequestManager,
    private val context: Context)
    : PagerAdapter() {

    private val currentInstantiateView = SparseArray<View>(5)

    override fun isViewFromObject(view: View, `object`: Any) = view == `object`
    override fun getCount() = pictures.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(context).inflate(R.layout.picture_fullscreen_item, container, false)
        setPicture(position, view)

        container.addView(view)
        return view
    }

    fun setPicture(position: Int, view: View? = null) {
        val containerView = view ?: currentInstantiateView[position] ?: return

        val picture = pictures[position]
        if(picture.file?.exists() == true) {
            if (picture.name.endsWith(".webp", true)) {
                runCatching {
                    PictureMetaContainer.readFromFile(picture.file).rotation
                }.onSuccess { rotation ->
                    GlideTools.loadWebpImageIntoView(glide, containerView.imageViewPicture, picture, context, rotation)
                }.onFailure {
                    GlideTools.loadImageIntoView(glide, containerView.imageViewPicture, picture, context)
                }
            } else {
                GlideTools.loadImageIntoView(glide, containerView.imageViewPicture, picture, context)
            }
        } else {
            GlideTools.setLoaderIntoView(glide, containerView.imageViewPicture, context)
            currentInstantiateView[position] = containerView
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val view = `object` as View
        glide.clear(view.imageViewPicture)
        currentInstantiateView.remove(position)
        container.removeView(view)
    }
}