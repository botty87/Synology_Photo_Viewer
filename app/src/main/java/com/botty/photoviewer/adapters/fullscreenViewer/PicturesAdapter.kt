package com.botty.photoviewer.adapters.fullscreenViewer

import android.content.Context
import android.graphics.Color
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import androidx.viewpager.widget.PagerAdapter
import com.botty.photoviewer.R
import com.botty.photoviewer.data.PictureContainer
import com.botty.photoviewer.data.PictureMetaContainer
import com.botty.photoviewer.galleryViewer.CacheMetadata
import com.botty.photoviewer.tools.*
import com.botty.photoviewer.tools.AppPreferences.showPictureInfo
import com.botty.photoviewer.tools.glide.GlideTools
import com.bumptech.glide.RequestManager
import com.github.florent37.kotlin.pleaseanimate.please
import kotlinx.android.synthetic.main.picture_fullscreen_item.view.*
import kotlinx.android.synthetic.main.picture_fullscreen_item.view.imageViewPicture
import kotlinx.android.synthetic.main.picture_item.view.*
import kotlin.math.absoluteValue

class PicturesAdapter(
    private val pictures: List<PictureContainer>,
    private val glide: RequestManager,
    private val picturesMetaCache: CacheMetadata,
    private val context: Context)
    : PagerAdapter() {

    //private val currentInstantiateView = SparseArray<View>(5)
    private val dateParser = Tools.standardDateParser

    override fun isViewFromObject(view: View, `object`: Any) = view == `object`
    override fun getCount() = pictures.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(context).inflate(R.layout.picture_fullscreen_item, container, false)
        setPicture(position, view)
        view.id = pictures[position].hashCode.absoluteValue
        container.addView(view)
        return view
    }

    fun setPicture(position: Int, containerView: View) {
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
            CircularProgressDrawable(context).apply {
                strokeWidth = 5f
                centerRadius = 30f
                this.setColorSchemeColors(Color.RED)
                start()
            }.let {prog ->
                containerView.imageViewPicture.setImageDrawable(prog)
            }
        }
        setPictureInfo(position, containerView)
    }

    fun setPictureInfo(pos: Int, containerView: View) {
        if(showPictureInfo) {
            containerView.run {
                val picture = pictures[pos]
                runCatching {
                    picturesMetaCache[picture.hashCode]
                }.onFailure {
                    textViewPictureDate.hide(true)
                }.onSuccess { picMetaData ->
                    textViewPictureDate.text = dateParser.format(picMetaData.originDate)
                    textViewPictureDate.show()
                }
                textViewPictureName.text = picture.name
                if(layoutInfo.isInvisible){
                    layoutInfo.show()
                }
            }
        } else {
            containerView.run {
                if(layoutInfo.isVisible){
                    layoutInfo.hide()
                }
            }
        }
    }

    fun changePictureVisibility(containerView: View) {
        showPictureInfo = !showPictureInfo
        please {
            animate(containerView.layoutInfo) {
                if(showPictureInfo)
                    visible()
                else
                    invisible()
            }
        }.start()
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val view = `object` as View
        view.id = View.NO_ID
        glide.clear(view.imageViewPicture)
        container.removeView(view)
    }
}