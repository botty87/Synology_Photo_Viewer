package com.botty.photoviewer.galleryViewer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
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
import com.botty.photoviewer.settings.Settings
import com.botty.photoviewer.tools.*
import com.bumptech.glide.Glide
import com.github.florent37.kotlin.pleaseanimate.please
import kotlinx.android.synthetic.main.activity_fullscreen_viewer.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val INFO_DELAY = 3000L

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
    private lateinit var picturesMetaCache: CacheMetadata
    private val pictures = mutableListOf<PictureContainer>()
    private var currentPicIndex = 0

    private var presentationHandler: Handler? = null
    private var hideInfoHandler: Handler? = null
    private val dateParser = Tools.standardDateParser

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
                picturesMetaCache = CacheMetadata(50, pictures)
                getParcelableArrayListExtra<PictureMetaContainer.ParcelablePair>(METADATA_CACHE_LIST_KEY)?.forEach { pictureMetaPair ->
                    picturesMetaCache.put(pictureMetaPair.hash, pictureMetaPair.pictureMetaContainer)
                } ?: throw Exception()
                currentPicIndex = getIntExtra(CURRENT_PICTURE_KEY, 0)
                picturesLoader.setNewGalleryPath(galleryPath)
            }
        } catch (e: Exception) {
            finish()
            return
        }

        picturesAdapter = PicturesAdapter(pictures, glide, this)
        viewPagerPicture.offscreenPageLimit = 2
        viewPagerPicture.adapter = picturesAdapter
        viewPagerPicture.currentItem = currentPicIndex

        picturesLoader.pictureNotifier.observe(this) { picIndex ->
            findChildViewAdapter(picIndex)?.run { picturesAdapter.setPicture(picIndex, this) }
            if(picIndex == viewPagerPicture.currentItem) {
                setPictureInfo(picIndex)
            }
        }

        picturesLoader.startDownload(currentPicIndex)
        hideInfo()

        viewPagerPicture.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                picturesLoader.startDownload(position)
                launch {
                    setPictureInfo(position)
                }
            }
        })

        var viewPagerLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
        viewPagerLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            viewPagerPicture.viewTreeObserver.removeOnGlobalLayoutListener(viewPagerLayoutListener)
            viewPagerLayoutListener = null
            setPictureInfo(viewPagerPicture.currentItem)
        }
        viewPagerPicture.viewTreeObserver.addOnGlobalLayoutListener(viewPagerLayoutListener)

        buttonStartPresentation.setOnClickListener {
            if(presentationHandler.isNull()) {
                val pauseDrawable = ContextCompat.getDrawable(this, R.drawable.ic_pause_circle_24dp)
                buttonStartPresentation.setImageDrawable(pauseDrawable)
                startPresentation()
            } else {
                presentationHandler!!.removeCallbacksAndMessages(null)
                presentationHandler = null
                val startDrawable = ContextCompat.getDrawable(this, R.drawable.ic_play_circle_24dp)
                buttonStartPresentation.setImageDrawable(startDrawable)
            }
        }

        buttonStartPresentation.setOnFocusChangeListener { view, focused ->
            if(focused) {
                hideInfo() //Reset the handler
                view.setBackgroundColor(Color.GRAY)
            } else {
                view.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    private fun startPresentation() {
        please {
            animate(buttonStartPresentation) {
                invisible()
            }
            withEndAction {
                viewPagerPicture.requestFocus()
                buttonStartPresentation.isFocusable = false
            }
        }
        presentationHandler = Handler().apply {
            postDelayed({
                var nextItem = viewPagerPicture.currentItem + 1
                if(nextItem == pictures.size) {
                    nextItem = 0
                }
                viewPagerPicture.currentItem = nextItem
                startPresentation()
            }, (Settings.presentationTimeout * 1000L))
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if(event?.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_DPAD_CENTER -> {
                    return if(buttonStartPresentation.isFocused) {
                        super.dispatchKeyEvent(event)
                    } else {
                        showInfo()
                        true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun cleanInfoHandler() {
        hideInfoHandler?.removeAll()
        hideInfoHandler = null
    }

    private fun setPictureInfo(pos: Int) {
        if(Settings.showPicInfoFullScreen) {
            cleanInfoHandler()
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
            please(100) {
                animate(layoutInfo) {
                    visible()
                }
            }.start()
            hideInfo()
        }
    }

    private fun hideInfo() {
        cleanInfoHandler()
        hideInfoHandler = Handler().apply {
            postDelayed({
                please(450) {
                    animate(layoutInfo) {
                        invisible()
                    }
                    animate(buttonStartPresentation) {
                        invisible()
                    }
                    withEndAction {
                        viewPagerPicture.requestFocus()
                        buttonStartPresentation.isFocusable = false
                    }
                }.start()
                hideInfoHandler = null
            }, INFO_DELAY)
        }
    }

    private fun showInfo() {
        cleanInfoHandler()
        please {
            animate(layoutInfo) {
                visible()
            }
            animate(buttonStartPresentation) {
                visible()
            }
            withEndAction {
                buttonStartPresentation.isFocusable = true
                hideInfo()
            }
        }.start()
    }

    private fun findChildViewAdapter(pos: Int = viewPagerPicture.currentItem): View? {
        val viewId = pictures[pos].hashCode.absoluteValue
        return viewPagerPicture.findViewById(viewId)
    }

    override fun onBackPressed() {
        picturesLoader.cancelDownload(true)
        val intent = Intent()
        intent.putExtra(CURRENT_PICTURE_KEY, viewPagerPicture.currentItem)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
