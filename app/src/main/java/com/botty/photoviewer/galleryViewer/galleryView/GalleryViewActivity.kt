package com.botty.photoviewer.galleryViewer.galleryView

import android.annotation.SuppressLint
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
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.galleryViewer.FoldersAdapter
import com.botty.photoviewer.adapters.galleryViewer.PicturesAdapter
import com.botty.photoviewer.components.*
import com.botty.photoviewer.components.workers.LogoutWorker
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.PictureMetaContainer
import com.botty.photoviewer.galleryViewer.fullscreenView.FullscreenViewerActivity
import com.botty.photoviewer.galleryViewer.loader.PicturesLoader
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

@SuppressLint("SimpleDateFormat")
class GalleryViewActivity : FragmentActivity(), CoroutineScope by MainScope() {

    private val foldersAdapter by lazy {
        recyclerViewFolders.run {
            layoutManager = LinearLayoutManager(this@GalleryViewActivity)
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(this@GalleryViewActivity, DividerItemDecoration.VERTICAL))
            setSelectPadding(5, 0, 5, 0)
        }

        FoldersAdapter().apply {
            fun onFolderFocused() {
                if(!isFoldersVisible && !isLoadingFolder) {
                    isFoldersVisible = true
                    recyclerViewPictures?.mFocusBorderView?.drawBorder = false
                    recyclerViewFolders.setItemSelected(0)
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
                        recyclerViewFolders.mFocusBorderView?.drawBorder = true
                        notifyDataSetChanged()
                    }.start()
                }
            }

            fun onFolderClick(position: Int) = launch {
                showLoader()
                galleryViewModel.onFolderClick(folders[position])
                setViewsAfterLoad()
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

                override fun onItemViewFocusChanged(gainFocus: Boolean, view: View?, position: Int) {
                    if(gainFocus) {
                        onFolderFocused()
                    }
                }
            })

            recyclerViewFolders.adapter = this
        }
    }

    private var isLoadingFolder: Boolean = false
    private var isFoldersVisible = true
    private var falsePhotoFocused = true

    private val picturesAdapter by lazy {
        fun onPhotoFocused() {
            if(isFoldersVisible && !isLoadingFolder && falsePhotoFocused) {
                val focusedView = currentFocus
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
                }.withStartAction {
                    recyclerViewFolders.mFocusBorderView?.drawBorder = false
                    viewGuide.requestFocus()
                    focusedView?.clearFocus()
                }.withEndAction {
                    isFoldersVisible = false
                    recyclerViewPictures?.mFocusBorderView?.drawBorder = true
                    focusedView?.requestFocus()
                }.start()
            }
        }

        val width = resources.getDimension(R.dimen.picture_size).toInt()
        PicturesAdapter(galleryViewModel.picturesMetaCache, this)
            .apply {
                setHasStableIds(true)
                recyclerViewPictures.layoutManager = GridAutofitLayoutManager(this@GalleryViewActivity, width)
                recyclerViewPictures.setHasFixedSize(true)
                recyclerViewPictures.itemAnimator = null
                recyclerViewPictures.adapter = this

                recyclerViewPictures.setOnItemStateListener(object : TvRecyclerView.OnItemStateListener {
                    override fun onItemViewClick(view: View?, position: Int) {
                        onPhotoClick(position)
                    }

                    override fun onItemViewFocusChanged(gainFocus: Boolean, view: View?, position: Int) {
                        if(gainFocus) {
                            onPhotoFocused()
                        }
                    }
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

    private var galleryId: Long = 0 //It is read in the "on create"
    private val galleryViewModel: GalleryViewModel by koinLifecycleScope.viewModel(this) { parametersOf(galleryId) }

    private val picturesLoader: PicturesLoader by koinLifecycleScope.viewModel(this) { parametersOf(galleryId) }

    override fun onCreate(savedInstanceState: Bundle?) {
        fun setLayoutFirstLaunch() {
            textViewAlbumName.text = galleryViewModel.gallery.name
            textViewAlbumNameTitle.hide()
            textViewAlbumName.text = getString(R.string.loading)
            recyclerViewPictures.mFocusBorderView?.drawBorder = false
            please {
                animate(recyclerViewPictures) {
                    rightOf(viewGuide)
                }
            }.now()
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
                initPictureObserver()
                setViewsAfterLoad()
            }.onFailure { e ->
                showErrorToast("${e.localizedMessage} ${getString(R.string.con_params_changed_error)}", Toasty.LENGTH_LONG)
                Handler().postDelayed({ finish() }, 6000)
            }
        }
    }

    private fun setViewsAfterLoad() {
        falsePhotoFocused = false
        //TODO renable???
        /*try {
            recyclerViewPictures.setItemSelected(0)
        } catch (e: Exception) {
            //Sometimes it could happens. In case go next
            e.log()
        }*/

        runCatching {
            recyclerViewFolders.setItemSelected(0)
            recyclerViewFolders.getChildAt(0)?.requestFocus()
            recyclerViewPictures.setItemSelected(-1)
        }.onFailure { e ->
            e.log()
        }
        hideLoader()
        Handler().postDelayed({
            falsePhotoFocused = true
        }, 600)
    }

    private fun initPictureObserver() {
        galleryViewModel.pictures.observe(this) { pictures ->
            picturesAdapter.setNewPictures(pictures)

            //picturesLoader.setNewGalleryPath(galleryViewModel.gallCont.currentGalleryPath) TODO remove?
            if(pictures.isEmpty()) {
                return@observe
            }

            var recyclerViewLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
            recyclerViewLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
                recyclerViewPictures.viewTreeObserver.removeOnGlobalLayoutListener(recyclerViewLayoutListener)
                recyclerViewLayoutListener = null
                picturesLoader.pictureNotifier.observe(this) { picIndex ->
                    recyclerViewPictures.adapter?.notifyItemChanged(picIndex)
                }
                startDownloadPictures(false)
            }
            recyclerViewPictures.viewTreeObserver.addOnGlobalLayoutListener(recyclerViewLayoutListener)
        }
    }

    private fun startDownloadPictures(withDelay: Boolean) {
        val firstPosition =
            (recyclerViewPictures.layoutManager as GridAutofitLayoutManager)
                .findFirstCompletelyVisibleItemPosition()
        val lastPosition =
            (recyclerViewPictures.layoutManager as GridAutofitLayoutManager)
                .findLastCompletelyVisibleItemPosition()
        picturesLoader.startDownload(firstPosition, lastPosition, withDelay)
    }

    private fun initFoldersObserver() {
        galleryViewModel.folders.observe(this) { folders ->
            val actualPath = galleryViewModel.actualPath
            when {
                actualPath.size > 1 -> {
                    foldersAdapter.setFolders(folders, actualPath[actualPath.size - 2])
                }
                actualPath.size == 1 -> {
                    foldersAdapter.setFolders(folders, galleryViewModel.gallery.name)
                }
                else -> {
                    foldersAdapter.setFolders(folders)
                }
            }
            textViewAlbumName.text = actualPath.lastOrNull() ?: galleryViewModel.gallery.name
        }
    }

    private fun hideLoader() {
        isLoadingFolder = false
        progressPicturesLoader.hide()
        recyclerViewFolders.show()
        recyclerViewFolders.mFocusBorderView?.show()
        recyclerViewPictures.mFocusBorderView?.show()
        recyclerViewPictures.show()
        textViewAlbumNameTitle.show()
    }

    private fun showLoader() {
        isLoadingFolder = true
        recyclerViewFolders.hide()
        recyclerViewPictures.hide()
        recyclerViewPictures.mFocusBorderView?.hide()
        recyclerViewFolders.mFocusBorderView?.hide()
        textViewAlbumNameTitle.hide()
        textViewAlbumName.text = getString(R.string.loading)
        progressPicturesLoader.show()
    }

    override fun onBackPressed() {
        if(galleryViewModel.actualPath.isEmpty()) {
            super.onBackPressed()
        } else {
            recyclerViewFolders.getChildAt(0).requestFocus()
            recyclerViewPictures.setItemSelected(-1)
            onParentFolderClick()
        }
    }

    private fun onParentFolderClick() {
        launch {
            showLoader()
            galleryViewModel.onParentClick()
            setViewsAfterLoad()
        }
    }

    private fun onPhotoClick(pos: Int) {
        picturesLoader.cancelDownload()
        val picturesCacheContainer = galleryViewModel.picturesMetaCache.snapshot().map { entry ->
            PictureMetaContainer.ParcelablePair(entry.key, entry.value)
        }

        startActivityForResult<FullscreenViewerActivity>(
            FullscreenViewerActivity.PICTURES_LIST_KEY to galleryViewModel.pictures.value,
            FullscreenViewerActivity.PICTURE_GALLERY_PATH_KEY to galleryViewModel.currentGalleryPath,
            FullscreenViewerActivity.SESSION_PARAMS_KEY to galleryViewModel.sessionParams,
            FullscreenViewerActivity.METADATA_CACHE_LIST_KEY to picturesCacheContainer,
            FullscreenViewerActivity.CURRENT_PICTURE_KEY to pos) { result ->

            val currentPos = result.data?.getIntExtra(FullscreenViewerActivity.CURRENT_PICTURE_KEY, 0) ?: 0
            recyclerViewPictures.setItemSelected(currentPos)
        }
    }

    override fun onStop() {
        LogoutWorker.setWorker(this, galleryViewModel.sessionParams)
        super.onStop()
    }
}