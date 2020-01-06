package com.botty.photoviewer.main

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.afollestad.materialdialogs.MaterialDialog
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.main.GalleriesAdapter
import com.botty.photoviewer.addGallery.AddShareActivity
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.ObjectBox
import com.botty.photoviewer.galleryViewer.GalleryViewActivity
import com.botty.photoviewer.settings.SettingsActivity
import com.botty.photoviewer.tools.*
import com.botty.photoviewer.tools.network.Network
import com.botty.photoviewer.tools.workers.scanGalleries.ScanGalleriesPref
import com.botty.photoviewer.tools.workers.scanGalleries.ScanGalleriesWorker
import com.bumptech.glide.Glide
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : FragmentActivity() {

    private val galleriesAdapter by lazy {
        GalleriesAdapter(Glide.with(this)).apply {
            onAddNewClick = this@MainActivity::onAddNewClick
            onGalleryClick = this@MainActivity::onGalleryClick
            onSettingsClick = this@MainActivity::onSettingsClick

            val width = resources.getDimension(R.dimen.gallery_width).toInt()
            recyclerViewGalleries.layoutManager = GridAutofitLayoutManager(this@MainActivity, width)
            recyclerViewGalleries.setHasFixedSize(true)
            recyclerViewGalleries.adapter = this
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadGalleries()
        checkGalleriesScan()
    }

    private fun checkGalleriesScan() {
        WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(ScanGalleriesWorker.TAG)
            .observe(this) { worksInfo ->
                if(!worksInfo.isEmpty()) {

                }
            }

        if(ScanGalleriesPref.isFirstSyncNeeded) {
            scanGalleries()
        }
    }

    private fun scanGalleries(galleryId: Long = 0) {
        layoutProgressLoader.show()
        Tools.scanGalleries(this, galleryId) { success ->
            layoutProgressLoader.hide(true)
            if(success) {
                showSuccessToast(R.string.scan_completed, Toasty.LENGTH_LONG)
            }
        }
    }

    private fun loadGalleries() {
        galleriesAdapter.setNewGalleries(ObjectBox.galleryBox.all)
    }

    private fun onAddNewClick() {
        if(!ScanGalleriesPref.isSyncingGalleries) {
            startActivityForResult<AddShareActivity> { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    loadGalleries()
                    val galleryId = result.data?.getLongExtra(Gallery.ID_TAG, 0) ?: 0
                    scanGalleries(galleryId)
                }
            }
        }
    }

    private fun onSettingsClick() {
        if(!ScanGalleriesPref.isSyncingGalleries) {
            startActivity<SettingsActivity>()
        }
    }

    private fun onGalleryClick(gallery: Gallery) {
        if(!ScanGalleriesPref.isSyncingGalleries) {
            startActivity<GalleryViewActivity>(Gallery.ID_TAG to gallery.id)
        }
    }

    override fun onDestroy() {
        Network.clean()
        super.onDestroy()
    }
}