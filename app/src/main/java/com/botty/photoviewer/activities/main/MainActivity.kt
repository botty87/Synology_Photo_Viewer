package com.botty.photoviewer.activities.main

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.observe
import com.botty.photoviewer.R
import com.botty.photoviewer.activities.addGallery.AddShareActivity
import com.botty.photoviewer.activities.galleryViewer.galleryView.GalleryViewActivity
import com.botty.photoviewer.activities.settings.SettingsActivity
import com.botty.photoviewer.adapters.main.GalleriesAdapter
import com.botty.photoviewer.components.*
import com.botty.photoviewer.components.views.GridAutofitLayoutManager
import com.botty.photoviewer.data.Gallery
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModel()

    private val galleriesAdapter by lazy {
        GalleriesAdapter(Glide.with(this)).apply {
            onAddNewClick = this@MainActivity::onAddNewClick
            onGalleryClick = this@MainActivity::onGalleryClick
            onSettingsClick = this@MainActivity::onSettingsClick

            val width = resources.getDimension(R.dimen.gallery_width).toInt()
            recyclerViewGalleries.layoutManager =
                GridAutofitLayoutManager(
                    this@MainActivity,
                    width
                )
            recyclerViewGalleries.setHasFixedSize(true)
            recyclerViewGalleries.adapter = this
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        adView.loadAdWithFailListener()

        mainViewModel.galleries.observe(this) { galleries ->
            galleriesAdapter.setNewGalleries(galleries)
        }

        mainViewModel.syncStatus.observe(this) { syncStatus ->
            if(syncStatus.active) {
                val progressMessage = StringBuilder(getString(R.string.gallery_sync_progress))
                progressMessage.appendln()
                progressMessage.append("${getString(R.string.completed)} ${syncStatus.completedGallery} ${getString(R.string.of)} ${syncStatus.totalGalleries}")
                textViewSyncProgress.text = progressMessage.toString()
                mainLayout.hide()
                syncLayout.show()
            } else {
                syncLayout.hide(true)
                mainLayout.show()
                syncStatus.errorMessage?.run { showErrorToast(this) }
            }
        }

        mainViewModel.checkDBSyncStatus()

        startActivity<GalleryViewActivity>(Gallery.ID_TAG to 1L) //TODO remove
    }

    private fun onAddNewClick() {
        startActivityForResult<AddShareActivity> {
            mainViewModel.checkDBSyncStatus()
        }
    }

    private fun onGalleryClick(gallery: Gallery) {
        startActivity<GalleryViewActivity>(Gallery.ID_TAG to gallery.id)
    }

    private fun onSettingsClick() {
        startActivityForResult<SettingsActivity> {
            mainViewModel.checkDBSyncStatus()
        }
    }
}