package com.botty.photoviewer.galleryViewer

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.galleryViewer.FoldersAdapter
import com.botty.photoviewer.adapters.galleryViewer.PicturesAdapter
import com.botty.photoviewer.data.*
import com.botty.photoviewer.galleryViewer.loader.PicturesLoader
import com.botty.photoviewer.tools.*
import com.botty.photoviewer.tools.network.Network
import com.botty.photoviewer.tools.network.responses.containers.Share
import com.botty.photoviewer.tools.workers.LogoutWorker
import com.botty.tvrecyclerview.TvRecyclerView
import com.bumptech.glide.Glide
import com.github.florent37.kotlin.pleaseanimate.please
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_gallery_view.*
import kotlinx.coroutines.*

@SuppressLint("SimpleDateFormat")
class GalleryViewActivity : FragmentActivity(), CoroutineScope by MainScope() {

    private val pictures = mutableListOf<PictureContainer>()
    private val picturesMetaCache by lazy { CacheMetadata(200, pictures) }

    private lateinit var gallery: Gallery
    private lateinit var sessionParams: SessionParams
    private val actualPath by lazy { mutableListOf<String>() }

    private val picturesLoader by lazy {
        ViewModelProvider(this,
            PicturesLoader.Factory(sessionParams, glideManager, pictures, 10))
            .get(PicturesLoader::class.java)
    }

    private val foldersAdapter by lazy {
        recyclerViewFolders.run {
            layoutManager = LinearLayoutManager(this@GalleryViewActivity)
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(this@GalleryViewActivity, DividerItemDecoration.VERTICAL))
            setSelectPadding(5, 0, 5, 0)
        }
        FoldersAdapter().apply {
            recyclerViewFolders.setOnItemStateListener(object : TvRecyclerView.OnItemStateListener {
                override fun onItemViewClick(view: View?, position: Int) {
                    if(parentName.isNotNull()) {
                        if(position == 0) {
                            onParentClick()
                        } else {
                            onFolderClick(folders[position - 1])
                        }
                    } else {
                        onFolderClick(folders[position])
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

    private val glideManager by lazy { Glide.with(this) }

    private val picturesAdapter by lazy {
        val width = resources.getDimension(R.dimen.picture_size).toInt()
        PicturesAdapter(glideManager, picturesMetaCache,pictures, this)
            .apply {
                setHasStableIds(true)
                recyclerViewPictures.layoutManager = GridAutofitLayoutManager(this@GalleryViewActivity, width)
                recyclerViewPictures.setHasFixedSize(true)
                recyclerViewPictures.itemAnimator = null
                recyclerViewPictures.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        when(newState) {
                            RecyclerView.SCROLL_STATE_IDLE -> {
                                startDownloadPictures()
                            }
                        }
                    }
                })

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
                recyclerViewPictures.setSelectPadding(5, 5, 5, 5)
            }
    }

    private lateinit var pictureGalleryPath: String

    private var isLoadingFolder: Boolean = false
    private var isFoldersVisible = true
    private var falsePhotoFocused = true

    override fun onCreate(savedInstanceState: Bundle?) {
        fun setAlbumDetails() {
            textViewAlbumName.text = gallery.name
            textViewAlbumNameTitle.hide()
            textViewAlbumName.text = getString(R.string.loading)
            isFoldersVisible = true
            recyclerViewPictures.mFocusBorderView?.drawBorder = false
            please {
                animate(recyclerViewPictures) {
                    rightOf(viewGuide)
                }
            }.now()
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery_view)
        val galleryId = savedInstanceState?.getLong(Gallery.ID_TAG, 0L) ?:
        intent.getLongExtra(Gallery.ID_TAG, 0L)

        if(galleryId == 0L) {
            finish()
            return
        }

        gallery = ObjectBox.galleryBox[galleryId]
        performLogin()

        setAlbumDetails()
    }

    private fun performLogin() {
        gallery.connectionParams.target.let { conParams ->
            launch {
                runCatching {
                    Network.login(conParams)
                }.onSuccess { response ->
                    sessionParams = conParams.toSessionParams(response.sid)
                    loadFolder(gallery.path)
                }.onFailure { e ->
                    e.log()
                    showErrorToast("${e.localizedMessage} ${getString(R.string.con_params_changed_error)}", Toasty.LENGTH_LONG)
                    Handler().postDelayed({ finish() }, 6000)
                }
            }
        }
    }

    private fun loadFolder(path: String, nameToAdd: String? = null) {
        fun showLoader() {
            isLoadingFolder = true
            recyclerViewFolders.hide()
            recyclerViewPictures.hide()
            recyclerViewPictures.mFocusBorderView?.hide()
            recyclerViewFolders.mFocusBorderView?.hide()
            textViewAlbumNameTitle.hide()
            textViewAlbumName.text = getString(R.string.loading)
            progressPicturesLoader.show()
        }

        fun hideLoader() {
            isLoadingFolder = false
            progressPicturesLoader.hide()
            recyclerViewFolders.show()
            recyclerViewFolders.mFocusBorderView?.show()
            recyclerViewPictures.mFocusBorderView?.show()
            recyclerViewPictures.show()
            textViewAlbumNameTitle.show()
        }

        fun Share.isPicture(): Boolean = name.endsWithNoCase(".webp") ||
                name.endsWithNoCase(".jpg") ||
                name.endsWithNoCase(".jpeg") ||
                name.endsWithNoCase(".png") ||
                name.endsWithNoCase(".tif") ||
                name.endsWithNoCase(".tiff") ||
                name.endsWithNoCase(".gif")

        launch {
            runCatching {
                showLoader()
                picturesLoader.pictureNotifier.removeObservers(this@GalleryViewActivity)
                picturesLoader.cancelDownload(true)
                Network.getFoldersContent(sessionParams, path)
            }.onSuccess { response ->
                pictureGalleryPath = path

                val folders = async(Dispatchers.Default) {
                    response.files.filter { file ->
                        file.isdir && file.isNotHidden()
                    }
                }
                val picturesName = async(Dispatchers.Default) {
                    response.files.filter { file ->
                        file.isPicture() && file.isNotHidden()
                    }.map { share ->
                        val hash = getPictureFileHash(share.name)
                        PictureContainer(share.name, hash)
                    }
                }

                nameToAdd?.run { actualPath.add(this) } ?: actualPath.removeLast()

                when {
                    actualPath.size > 1 -> {
                        foldersAdapter.setFolders(folders.await(), actualPath[actualPath.size - 2])
                    }
                    actualPath.size == 1 -> {
                        foldersAdapter.setFolders(folders.await(), gallery.name)
                    }
                    else -> {
                        foldersAdapter.setFolders(folders.await())
                    }
                }

                picturesName.await().run {
                    setNewPictures(this)
                }
                textViewAlbumName.text = actualPath.lastOrNull() ?: gallery.name

                falsePhotoFocused = false
                try {
                    recyclerViewPictures.setItemSelected(0)
                } catch (e: Exception) {
                    //Sometimes it could happens. In case go next
                    e.log()
                }
                recyclerViewFolders.setItemSelected(0)
                hideLoader()
                Handler().postDelayed({
                    falsePhotoFocused = true
                }, 320)
            }.onFailure { e ->
                e.log()
                hideLoader()
                showErrorToast(e.localizedMessage ?: getString(R.string.error))
            }
        }
    }

    private fun setNewPictures(newPictures: List<PictureContainer>) {
        pictures.clear()
        pictures.addAll(newPictures)
        picturesAdapter.notifyDataSetChanged()
        picturesLoader.setNewGalleryPath(pictureGalleryPath)
        if(pictures.isEmpty()) {
            return
        }

        var recyclerViewLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
        recyclerViewLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            recyclerViewPictures.viewTreeObserver.removeOnGlobalLayoutListener(recyclerViewLayoutListener)
            recyclerViewLayoutListener = null
            picturesLoader.pictureNotifier.observe(this) { picIndex ->
                recyclerViewPictures.adapter?.notifyItemChanged(picIndex)
            }
            startDownloadPictures()
        }
        recyclerViewPictures.viewTreeObserver.addOnGlobalLayoutListener(recyclerViewLayoutListener)
    }

    private fun onFolderClick(folder: Share) {
        loadFolder(folder.path, folder.name)
    }

    private fun onParentClick() {
        if(actualPath.size <= 1) {
            loadFolder(gallery.path)
        } else {
            var path = "${gallery.path}/"
            for(i in 0 until actualPath.size - 1) {
                path += "${actualPath[i]}/"
            }
            path = path.dropLast(1)
            loadFolder(path)
        }
    }

    private suspend fun getPictureFileHash(picName: String) = withContext(Dispatchers.Default) {
        "$pictureGalleryPath/$picName".hashCode()
    }

    @SuppressLint("RtlHardcoded")
    private fun onPhotoFocused() {
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

    private fun onFolderFocused() {
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
            }.withEndAction {
                recyclerViewFolders.mFocusBorderView?.drawBorder = true
                foldersAdapter.notifyDataSetChanged()
            }.start()
        }
    }

    private fun onPhotoClick(pos: Int) {
        picturesLoader.cancelDownload(false)
        val picturesCacheContainer = picturesMetaCache.snapshot().map { entry ->
            PictureMetaContainer.ParcelablePair(entry.key, entry.value)
        }

        startActivityForResult<FullscreenViewerActivity>(
            FullscreenViewerActivity.PICTURES_LIST_KEY to pictures,
            FullscreenViewerActivity.METADATA_CACHE_LIST_KEY to picturesCacheContainer,
            FullscreenViewerActivity.PICTURE_GALLERY_PATH_KEY to pictureGalleryPath,
            FullscreenViewerActivity.SESSION_PARAMS_KEY to sessionParams,
            FullscreenViewerActivity.CURRENT_PICTURE_KEY to pos) { result ->

            val currentPos = result.data?.getIntExtra(FullscreenViewerActivity.CURRENT_PICTURE_KEY, 0) ?: 0
            recyclerViewPictures.setItemSelected(currentPos)
        }
    }

    fun startDownloadPictures() {
        val firstPosition =
            (recyclerViewPictures.layoutManager as GridAutofitLayoutManager)
                .findFirstCompletelyVisibleItemPosition()
        val lastPosition =
            (recyclerViewPictures.layoutManager as GridAutofitLayoutManager)
                .findLastCompletelyVisibleItemPosition()

        picturesLoader.startDownload(firstPosition, lastPosition)
    }

    override fun onBackPressed() {
        if(actualPath.isEmpty()) {
            super.onBackPressed()
        } else {
            recyclerViewFolders.getChildAt(0).requestFocus()
            recyclerViewPictures.setItemSelected(-1)
            onParentClick()
        }
    }

    override fun onStop() {
        if(isFinishing) {
            LogoutWorker.setWorker(this, sessionParams)
        }
        super.onStop()
    }
}