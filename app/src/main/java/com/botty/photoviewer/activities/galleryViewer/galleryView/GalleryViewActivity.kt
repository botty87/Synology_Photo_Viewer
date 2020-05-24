package com.botty.photoviewer.activities.galleryViewer.galleryView

import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botty.photoviewer.MyApplication
import com.botty.photoviewer.R
import com.botty.photoviewer.activities.galleryViewer.fullscreenView.FullscreenViewerActivity
import com.botty.photoviewer.activities.galleryViewer.loader.PicturesLoader
import com.botty.photoviewer.adapters.galleryViewer.FoldersAdapter
import com.botty.photoviewer.adapters.galleryViewer.PicturesAdapter
import com.botty.photoviewer.components.*
import com.botty.photoviewer.components.views.GridAutofitLayoutManager
import com.botty.photoviewer.components.workers.LogoutWorker
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.PictureMetaContainer
import com.botty.tvrecyclerview.TvRecyclerView
import com.github.florent37.kotlin.pleaseanimate.please
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_gallery_view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.scope.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.androidx.scope.lifecycleScope as koinLifecycleScope

class GalleryViewActivity : FragmentActivity(), CoroutineScope by MainScope() {

    private var isFullscreenViewerActivityOpened = false

    private var galleryId: Long = 0 //It is read in the "on create"
    private val galleryViewModel: GalleryViewModel by koinLifecycleScope.viewModel(this) { parametersOf(galleryId) }
    private val picturesLoader: PicturesLoader by koinLifecycleScope.viewModel(this) { parametersOf(galleryId) }

    private val foldersAdapter by lazy {
        recyclerViewFolders.run {
            layoutManager = LinearLayoutManager(this@GalleryViewActivity)
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(this@GalleryViewActivity, DividerItemDecoration.VERTICAL))
            setSelectPadding(5, 0, 5, 0)
        }

        FoldersAdapter().apply {
            fun onFolderClick(position: Int) = launch {
                val pippo = position
                val folder = folders[position]
                showLoader()
                galleryViewModel.onFolderClick(folder)
                hideLoader()
            }

            recyclerViewFolders.setOnItemStateListener(object : TvRecyclerView.OnItemStateListener {
                override fun onItemViewClick(view: View?, position: Int) {
                    if(parentName.isNotNull()) {
                        if(position == 0) {
                            onParentFolderClick()
                        } else {
                            onFolderClick(position-1)
                        }
                    } else {
                        onFolderClick(position)
                    }
                }

                override fun onItemViewFocusChanged(gainFocus: Boolean, view: View?, position: Int) {}
            })

            recyclerViewFolders.adapter = this
        }
    }

    private val picturesAdapter by lazy {
        val width = resources.getDimension(R.dimen.picture_size).toInt()
        PicturesAdapter(galleryViewModel.picturesMetaCache, this)
            .apply {
                setHasStableIds(true)
                recyclerViewPictures.layoutManager = GridAutofitLayoutManager(this@GalleryViewActivity, width)
                recyclerViewPictures.setHasFixedSize(true)
                recyclerViewPictures.itemAnimator = null

                recyclerViewPictures.setOnItemStateListener(object : TvRecyclerView.OnItemStateListener {
                    override fun onItemViewClick(view: View?, position: Int) {
                        onPhotoClick(position)
                    }

                    override fun onItemViewFocusChanged(gainFocus: Boolean, view: View?, position: Int) {}
                })

                recyclerViewPictures.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        when(newState) {
                            RecyclerView.SCROLL_STATE_IDLE -> {
                                startDownloadPictures(true)
                            }
                        }
                    }
                })

                recyclerViewPictures.setSelectPadding(5, 5, 5, 5)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        fun setLayoutFirstLaunch() {
            textViewAlbumNameTitle.hide()
            textViewAlbumName.text = getString(R.string.loading)

            please {
                animate(recyclerViewPictures) {
                    rightOf(viewGuide)
                }
            }.now()
        }

        var firstDraw = true
        fun setFoldersOnFocusChange() {
            recyclerViewFolders.onFocusChangeListener = View.OnFocusChangeListener { _, focused ->
                if(focused) {
                    if(firstDraw) {
                        firstDraw = false
                        return@OnFocusChangeListener
                    }

                    recyclerViewFolders.mFocusBorderView?.hide()
                    please {
                        animate(navDetailsLayout) {
                            visible()
                            originalPosition()
                        }
                        animate(recyclerViewPictures) {
                            rightOf(viewGuide)
                        }
                        animate(adView) {
                            originalPosition()
                        }
                    }.withEndAction {
                        recyclerViewFolders.mFocusBorderView?.show()
                    }.start()
                }
            }
        }

        fun setPicturesOnFocusChange() {
            recyclerViewPictures.onFocusChangeListener = View.OnFocusChangeListener { _, focused ->
                if(focused) {
                    recyclerViewPictures.mFocusBorderView?.hide()
                    please {
                        animate(recyclerViewPictures) {
                            originalPosition()
                        }
                        animate(navDetailsLayout) {
                            outOfScreen(Gravity.LEFT)
                            alpha(0.1f)
                        }
                        animate(adView) {
                            centerHorizontalInParent()
                        }
                    }.withEndAction {
                        recyclerViewPictures.mFocusBorderView?.show()
                    }.start()
                }
            }
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery_view)
        adView.loadAdWithFailListener()

        galleryId = savedInstanceState?.getLong(Gallery.ID_TAG, 0L) ?:
                intent.getLongExtra(Gallery.ID_TAG, 0L)

        if(galleryId == 0L) {
            finish()
            return
        }

        setLayoutFirstLaunch()

        launch {
            runCatching {
                galleryViewModel.performLoginAndLoadFolder()
            }.onSuccess {
                initFoldersObserver()
                hideLoader()
                initPictureObserver()
            }.onFailure { e ->
                showErrorToast("${e.localizedMessage} ${getString(R.string.con_params_changed_error)}", Toasty.LENGTH_LONG)
                Handler().postDelayed({ finish() }, 6000)
            }
        }

        setPicturesOnFocusChange()
        setFoldersOnFocusChange()
    }

    private fun initFoldersObserver() {
        galleryViewModel.folders.observe(this) { folders ->
            val pathTree = galleryViewModel.pathTree
            when {
                pathTree.size > 2 -> {
                    foldersAdapter.setFolders(folders, pathTree[pathTree.size - 2])
                }
                pathTree.size == 2 -> {
                    foldersAdapter.setFolders(folders, galleryViewModel.gallery.name)
                }
                else -> {
                    foldersAdapter.setFolders(folders)
                }
            }
            textViewAlbumName.text = if(pathTree.size > 1) {
                pathTree.last()
            } else {
                galleryViewModel.gallery.name
            }
        }
    }

    private fun initPictureObserver() {
        galleryViewModel.pictures.observe(this) { pictures ->
            //Useful to force clean
            recyclerViewPictures.adapter = null
            picturesAdapter.setNewPictures(pictures)
            recyclerViewPictures.adapter = picturesAdapter
            picturesLoader.pictureNotifier.observe(this) { picIndex ->
                recyclerViewPictures.adapter?.notifyItemChanged(picIndex)
            }
            if(pictures.isEmpty()) {
                return@observe
            }
            startDownloadPictures(false)
        }
    }

    private fun startDownloadPictures(withDelay: Boolean) {
        val firstPosition = (recyclerViewPictures.layoutManager as GridAutofitLayoutManager)
                .findFirstCompletelyVisibleItemPosition()
        val lastPosition = (recyclerViewPictures.layoutManager as GridAutofitLayoutManager)
                .findLastCompletelyVisibleItemPosition()
        picturesLoader.startDownload(firstPosition, lastPosition, withDelay)
    }

    private fun onParentFolderClick() {
        launch {
            showLoader()
            galleryViewModel.onParentClick()
            hideLoader()
        }
    }

    private fun hideLoader() {
        progressPicturesLoader.hide()
        recyclerViewFolders.show()
        recyclerViewPictures.show()
        textViewAlbumNameTitle.show()
        textViewAlbumName.show()
        recyclerViewFolders.isFocusable = true
        recyclerViewFolders.requestFocus()
        recyclerViewPictures.isFocusable = true
        recyclerViewFolders.mFocusBorderView?.show()
        recyclerViewPictures.mFocusBorderView?.show()

        lateinit var recyclerViewFoldersLayoutList: ViewTreeObserver.OnGlobalLayoutListener
        recyclerViewFoldersLayoutList = ViewTreeObserver.OnGlobalLayoutListener {
            Handler().postDelayed({recyclerViewFolders.setItemSelected(0)}, 100)
            recyclerViewFolders.viewTreeObserver.removeOnGlobalLayoutListener(recyclerViewFoldersLayoutList)
        }

        recyclerViewFolders.viewTreeObserver.addOnGlobalLayoutListener(recyclerViewFoldersLayoutList)
    }

    private fun showLoader() {
        recyclerViewPictures.isFocusable = false
        recyclerViewFolders.isFocusable = false
        recyclerViewFolders.hide()
        recyclerViewPictures.hide()
        textViewAlbumNameTitle.hide()
        textViewAlbumName.hide()
        recyclerViewFolders.mFocusBorderView?.hide()
        recyclerViewPictures.mFocusBorderView?.hide()
        progressPicturesLoader.show()
    }

    private fun onPhotoClick(pos: Int) {
        picturesLoader.cancelDownload()
        val picturesCacheContainer = galleryViewModel.picturesMetaCache.snapshot().map { entry ->
            PictureMetaContainer.ParcelablePair(entry.key, entry.value)
        }

        isFullscreenViewerActivityOpened = true

        startActivityForResult<FullscreenViewerActivity>(
            FullscreenViewerActivity.PICTURES_LIST_KEY to galleryViewModel.pictures.value,
            FullscreenViewerActivity.PICTURE_GALLERY_PATH_KEY to galleryViewModel.currentGalleryPath,
            FullscreenViewerActivity.SESSION_PARAMS_KEY to galleryViewModel.sessionParams,
            FullscreenViewerActivity.METADATA_CACHE_LIST_KEY to picturesCacheContainer,
            FullscreenViewerActivity.CURRENT_PICTURE_KEY to pos) { result ->

            isFullscreenViewerActivityOpened = false
            val currentPos = result.data?.getIntExtra(FullscreenViewerActivity.CURRENT_PICTURE_KEY, 0) ?: 0
            recyclerViewPictures.setItemSelected(currentPos)
        }.onFailed {
            isFullscreenViewerActivityOpened = false
        }
    }

    override fun onBackPressed() {
        if(galleryViewModel.pathTree.size == 1) {
            super.onBackPressed()
        } else {
            onParentFolderClick()
        }
    }

    override fun onResume() {
        super.onResume()
        (application as MyApplication).isGalleryViewerOpened = true
    }

    override fun onPause() {
        if(!isFullscreenViewerActivityOpened) {
            LogoutWorker.setWorker(this, galleryViewModel.sessionParams)
            (application as MyApplication).isGalleryViewerOpened = false
        }
        super.onPause()
    }
}