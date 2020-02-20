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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.afollestad.materialdialogs.MaterialDialog
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.galleryViewer.FoldersAdapter
import com.botty.photoviewer.adapters.galleryViewer.PicturesAdapter
import com.botty.photoviewer.adapters.galleryViewer.pictureAdapter.HeaderItem
import com.botty.photoviewer.adapters.galleryViewer.pictureAdapter.InfiniteScrollListener
import com.botty.photoviewer.adapters.galleryViewer.pictureAdapter.PictureItem
import com.botty.photoviewer.data.*
import com.botty.photoviewer.data.fileStructure.MediaFile
import com.botty.photoviewer.data.fileStructure.MediaFolder
import com.botty.photoviewer.galleryViewer.loader.PicturesLoader
import com.botty.photoviewer.tools.*
import com.botty.photoviewer.tools.network.Network
import com.botty.photoviewer.tools.workers.LogoutWorker
import com.botty.photoviewer.tools.workers.scanGalleries.ScanGalleriesPref
import com.botty.photoviewer.tools.workers.scanGalleries.ScanGalleriesWorker
import com.botty.tvrecyclerview.TvRecyclerView
import com.bumptech.glide.Glide
import com.github.florent37.kotlin.pleaseanimate.please
import com.google.android.flexbox.FlexboxLayoutManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_gallery_view.*
import kotlinx.coroutines.*

//TODO LazyThreadSafetyMode.NONE

@SuppressLint("SimpleDateFormat")
class GalleryViewActivity : FragmentActivity(), CoroutineScope by MainScope() {

    private enum class NavDirection {NEXT, BACK, NONE}

    private val galleryContainers by lazy { mutableListOf<GalleryContainer>() }
    private val picturesMetaCache by lazy { CacheMetadata(200, galleryContainers) }

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

    /*private val picturesAdapter by lazy {
        val width = resources.getDimension(R.dimen.picture_size).toInt()
        PicturesAdapter(glideManager, picturesMetaCache, pictures, this)
            .apply {
                setHasStableIds(true)
                recyclerViewPictures.layoutManager = GridAutofitLayoutManager(this@GalleryViewActivity, width)
                recyclerViewPictures.setHasFixedSize(true)
                recyclerViewPictures.itemAnimator = null
                recyclerViewPictures.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        when(newState) {
                            RecyclerView.SCROLL_STATE_IDLE -> {
                                startDownloadPictures(true)
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
    }*/

    private val layoutManager by lazy {
        val width = resources.getDimension(R.dimen.picture_size).toInt()
        GridAutofitLayoutManager(this, width)
    }

    private val newPicturesAdapter by lazy {
        GroupAdapter<GroupieViewHolder>().also { adapter ->

            /*val layoutManager = GridLayoutManager(this@GalleryViewActivity, 4).apply {
                spanSizeLookup = adapter.spanSizeLookup
            }*/

            layoutManager.spanSizeLookup = adapter.spanSizeLookup
            recyclerViewPictures.layoutManager = layoutManager

            recyclerViewPictures.setHasFixedSize(true)
            recyclerViewPictures.itemAnimator = null
            /*recyclerViewPictures.addOnScrollListener(object : InfiniteScrollListener(layoutManager) {
                override fun onLoadMore(current_page: Int) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

            })*/


            /*recyclerViewPictures.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    when(newState) {
                        RecyclerView.SCROLL_STATE_IDLE -> {
                            startDownloadPictures(true)
                        }
                    }
                }
            })*/

            recyclerViewPictures.adapter = adapter
            recyclerViewPictures.setOnItemStateListener(object : TvRecyclerView.OnItemStateListener {
                override fun onItemViewClick(view: View?, position: Int) {
                    //onPhotoClick(position) TODO restore!
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

    private var downloadPicturesHandler: Handler? = null

    private val currentFolderPath: String
        get() {
            var folderPath = gallery.path
            actualPath.forEach { path ->
                folderPath += "/${path}"
            }
            return folderPath
        }

    private lateinit var currentFolder: MediaFolder

    private var isLoadingFolder: Boolean = false
    private var isFoldersVisible = true
    private var falsePhotoFocused = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery_view)
        val galleryId = savedInstanceState?.getLong(Gallery.ID_TAG, 0L) ?:
        intent.getLongExtra(Gallery.ID_TAG, 0L)

        if(galleryId == 0L) {
            finish()
            return
        }

        please {
            animate(recyclerViewPictures) {
                rightOf(viewGuide)
            }
        }.now()
        gallery = ObjectBox.galleryBox[galleryId]
        performLogin()
        buttonSync.setOnClickListener {
            onSyncGalleryClick()
        }
    }

    private fun setButtonViewMode() {

        fun setBackground() {
            if(GalleryPreferences.showSubfolders) {
                buttonViewMode.setImageResource(R.drawable.ic_collections_30dp)
            } else {
                buttonViewMode.setImageResource(R.drawable.ic_image_30dp)
            }
        }

        buttonViewMode.setOnClickListener {
            GalleryPreferences.showSubfolders = !GalleryPreferences.showSubfolders
            setBackground()
            loadFoldersAndPictures(NavDirection.NONE)
        }

        GalleryPreferences.showSubfolders = false
        setBackground()
    }

    private fun performLogin() {
        gallery.connectionParams.target.let { conParams ->
            launch {
                textViewAlbumNameTitle.hide()
                textViewAlbumName.text = getString(R.string.loading)
                runCatching {
                    Network.login(conParams)
                }.onSuccess { response ->
                    sessionParams = conParams.toSessionParams(response.sid)
                    textViewAlbumNameTitle.show()
                    textViewAlbumName.clear()
                    buttonSync.show()
                    buttonViewMode.show()
                    setButtonViewMode()
                    currentFolder = gallery.folder.target
                    loadFoldersAndPictures(NavDirection.NONE)
                }.onFailure { e ->
                    e.log()
                    showErrorToast("${e.localizedMessage} ${getString(R.string.con_params_changed_error)}", Toasty.LENGTH_LONG)
                    Handler().postDelayed({ finish() }, 6000)
                }
            }
        }
    }

    private fun loadFoldersAndPictures(navDirection: NavDirection) {
        launch {
            when(navDirection) {
                NavDirection.NEXT -> actualPath.add(currentFolder.name)
                NavDirection.BACK -> actualPath.removeLast()
            }

            val folders = async(Dispatchers.IO) {
                currentFolder.childFolders
            }

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
            recyclerViewFolders.setItemSelected(0)
            textViewAlbumName.text = actualPath.lastOrNull() ?: gallery.name

            //picturesLoader.pictureNotifier.removeObservers(this@GalleryViewActivity)
            //picturesLoader.cancelDownload(true)

            /*val newPictures = async(Dispatchers.IO) {
                if(GalleryPreferences.showSubfolders) {
                    val pictures = mutableListOf<MediaFile>()

                    fun addPictures(folder: MediaFolder, basePath: String) {
                        val title = "$basePath\\${folder.name}"
                        if(folder.childFiles.isNotEmpty()) {
                            pictures.add(MediaFile.getHeaderMediaFile(title))
                            pictures.addAll(folder.childFiles)
                        }
                        /*folder.childFolders.forEach { childFolder ->
                            addPictures(childFolder, title)
                        }*/
                    }

                    runBlocking {
                        addPictures(currentFolder, currentFolderPath)
                    }

                    pictures
                }
                else {
                    currentFolder.childFiles
                }
            }*/

            newPicturesAdapter.clear()
            withContext(Dispatchers.IO) {
                galleryContainers.clear()
                if(GalleryPreferences.showSubfolders) {
                    //TODO implements
                } else {
                    galleryContainers.add(GalleryContainer(currentFolder.name, currentFolderPath, currentFolder.childFiles))
                    val section = Section()
                    val test = layoutManager.spanCount
                    section.setHeader(HeaderItem(currentFolder.name, test))
                    galleryContainers[0].pictures.map { picture ->
                        PictureItem(picture, PictureItem.ResLoader(glideManager, picturesMetaCache, this@GalleryViewActivity))
                    }.run {
                        section.addAll(this)
                    }
                    newPicturesAdapter.add(section)
                }
            }

            //picturesAdapter.notifyDataSetChanged()

            //picturesLoader.setNewGalleryPath(currentFolderPath)

            /*if(pictures.isEmpty()) {
                return@launch
            }*/

            /*var recyclerViewLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
            recyclerViewLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
                recyclerViewPictures.viewTreeObserver.removeOnGlobalLayoutListener(recyclerViewLayoutListener)
                recyclerViewLayoutListener = null
                picturesLoader.pictureNotifier.observe(this@GalleryViewActivity) { picIndex ->
                    recyclerViewPictures.adapter?.notifyItemChanged(picIndex)
                }
                startDownloadPictures(false)
            }
            recyclerViewPictures.viewTreeObserver.addOnGlobalLayoutListener(recyclerViewLayoutListener)*/
        }
    }

    private fun onFolderClick(folder: MediaFolder) {
        currentFolder = folder
        loadFoldersAndPictures(NavDirection.NEXT)
    }

    private fun onParentClick() {
        currentFolder = currentFolder.parentFolder.target
        loadFoldersAndPictures(NavDirection.BACK)
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

    /*private fun onPhotoClick(pos: Int) {
        picturesLoader.cancelDownload(false)
        val picturesCacheContainer = picturesMetaCache.snapshot().map { entry ->
            PictureMetaContainer.ParcelablePair(entry.key, entry.value)
        }

        startActivityForResult<FullscreenViewerActivity>(
            FullscreenViewerActivity.FOLDER_ID_KEY to currentFolder.id,
            FullscreenViewerActivity.METADATA_CACHE_LIST_KEY to picturesCacheContainer,
            FullscreenViewerActivity.PICTURE_GALLERY_PATH_KEY to currentFolderPath,
            FullscreenViewerActivity.SESSION_PARAMS_KEY to sessionParams,
            FullscreenViewerActivity.CURRENT_PICTURE_KEY to pos) { result ->

            val currentPos = result.data?.getIntExtra(FullscreenViewerActivity.CURRENT_PICTURE_KEY, 0) ?: 0
            recyclerViewPictures.setItemSelected(currentPos)
        }
    }*/

    /*private fun startDownloadPictures(withDelay: Boolean) {
        downloadPicturesHandler?.removeCallbacksAndMessages(null)
        downloadPicturesHandler = null

        val runnable = Runnable {
            //TODO Check!
            /*val firstPosition =
                (recyclerViewPictures.layoutManager as GridAutofitLayoutManager)
                    .findFirstCompletelyVisibleItemPosition()
            val lastPosition =
                (recyclerViewPictures.layoutManager as GridAutofitLayoutManager)
                    .findLastCompletelyVisibleItemPosition()*/

            (recyclerViewPictures.layoutManager as FlexboxLayoutManager).run {
                val firstPosition = findFirstCompletelyVisibleItemPosition()
                val lastPosition = findLastCompletelyVisibleItemPosition()

                picturesLoader.startDownload(firstPosition, lastPosition)
                downloadPicturesHandler = null
            }
        }

        if(withDelay) {
            downloadPicturesHandler = Handler().apply {
                postDelayed(runnable, 700)
            }
        } else {
            runnable.run()
        }
    }*/

    private fun onSyncGalleryClick() {
        fun startSync() {
            mainLayout.hide()
            scanLoaderView.show()

            //It means that this is the main gallery folder. Otherwise start the sync only from current folder
            val folderId = if(currentFolder.parentFolder.targetId == 0L) {
                0L
            } else {
                currentFolder.id
            }

            val id = ScanGalleriesWorker.setWorker(this, gallery.id, folderId)
            WorkManager.getInstance(this).getWorkInfoByIdLiveData(id)
                .observe(this) { workInfo ->
                    when(workInfo?.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            mainLayout.show()
                            scanLoaderView.hide(true)
                            showSuccessToast(R.string.scan_completed)
                            val newFolderId = workInfo.outputData.getLong(ScanGalleriesWorker.FOLDER_ID,
                                ObjectBox.galleryBox[gallery.id].folder.targetId)

                            currentFolder = ObjectBox.mediaFolderBox[newFolderId]
                            loadFoldersAndPictures(NavDirection.NONE)
                        }

                        WorkInfo.State.FAILED -> {
                            var errorMessage = workInfo.outputData.getString(ScanGalleriesWorker.ERROR_KEY)
                            errorMessage = "${getString(R.string.scan_error)}: $errorMessage\n${getString(
                                R.string.retry_scan)}"
                            MaterialDialog(this).show {
                                title(R.string.error)
                                message(text = errorMessage)
                                positiveButton(R.string.yes) {
                                    startSync()
                                }
                                negativeButton(R.string.no)
                            }
                            scanLoaderView.hide(true)
                            mainLayout.show()
                        }
                    }
                }
        }

        val message = "${getString(R.string.do_you_want_scan)} ${currentFolder.name} ${getString(R.string.and_all_subfolders)}?"
        MaterialDialog(this).show {
            title(R.string.gallery_scan)
            message(text = message)
            positiveButton(R.string.yes) {
                startSync()
            }
            negativeButton(R.string.no)
        }
    }

    override fun onBackPressed() {
        if(actualPath.isEmpty()) {
            super.onBackPressed()
        } else {
            recyclerViewPictures.setItemSelected(-1)
            onParentClick()
            recyclerViewFolders.getChildAt(0).requestFocus()
        }
    }

    override fun onStop() {
        if(isFinishing) {
            ScanGalleriesPref.isGalleryOpened = false
            if(::sessionParams.isInitialized)
                LogoutWorker.setWorker(this, sessionParams)
        }
        super.onStop()
    }
}