package com.botty.photoviewer.galleryViewer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.viewpager.widget.ViewPager
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.fullscreenViewer.PicturesAdapter
import com.botty.photoviewer.data.PictureContainer
import com.botty.photoviewer.data.PictureMetaContainer
import com.botty.photoviewer.data.SessionParams
import com.botty.photoviewer.galleryViewer.loader.PicturesLoader
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_fullscreen_viewer.*
import kotlinx.coroutines.*

class FullscreenViewerActivity : FragmentActivity(), CoroutineScope by MainScope() {

    companion object {
        const val PICTURES_LIST_KEY = "pic_list"
        const val METADATA_CACHE_LIST_KEY = "meta_cache"
        const val CURRENT_PICTURE_KEY = "cur_pic"
        const val PICTURE_GALLERY_PATH_KEY = "pic_gallery_path"
        const val SESSION_PARAMS_KEY = "ses_params"
    }

    private lateinit var picturesAdapter: PicturesAdapter
    private lateinit var sessionParams: SessionParams
    private lateinit var galleryPath: String
    private val pictures = mutableListOf<PictureContainer>()
    private val picturesMetaCache by lazy { CacheMetadata(50, pictures) }
    private var currentPicIndex = 0

    private val glide by lazy {
        Glide.with(this@FullscreenViewerActivity)
    }
    private val picturesLoader by lazy {
        ViewModelProvider(this,
            PicturesLoader.Factory(sessionParams, glide, pictures,5))
            .get(PicturesLoader::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_viewer)

        try{
            intent.run {
                galleryPath = getStringExtra(PICTURE_GALLERY_PATH_KEY) ?: throw Exception()
                sessionParams = getParcelableExtra(SESSION_PARAMS_KEY) ?: throw Exception()
                pictures.addAll(getParcelableArrayListExtra(PICTURES_LIST_KEY) ?: throw Exception())
                getParcelableArrayListExtra<PictureMetaContainer.ParcelablePair>(METADATA_CACHE_LIST_KEY)?.run {
                    forEach {pictureMetaPair ->
                        picturesMetaCache.put(pictureMetaPair.hash, pictureMetaPair.pictureMetaContainer)
                    }
                } ?: throw Exception()
                currentPicIndex = getIntExtra(CURRENT_PICTURE_KEY, 0)
                picturesLoader.setNewGalleryPath(galleryPath)
            }
        } catch (e: Exception) {
            finish()
            return
        }

        picturesAdapter = PicturesAdapter(pictures, glide, this@FullscreenViewerActivity)
        viewPagerPicture.offscreenPageLimit = 2
        viewPagerPicture.adapter = picturesAdapter
        viewPagerPicture.currentItem = currentPicIndex

        picturesLoader.pictureNotifier.observe(this) { picIndex ->
            picturesAdapter.setPicture(picIndex)
        }

        picturesLoader.startDownload(currentPicIndex)

        viewPagerPicture.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) = picturesLoader.startDownload(position)
        })
    }

    override fun onBackPressed() {
        picturesLoader.cancelDownload(true)
        val intent = Intent()
        intent.putExtra(CURRENT_PICTURE_KEY, viewPagerPicture.currentItem)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
