package com.botty.photoviewer.main

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.main.GalleriesAdapter
import com.botty.photoviewer.addGallery.AddShareActivity
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.galleryViewer.GalleryViewActivity
import com.botty.photoviewer.settings.SettingsActivity
import com.botty.photoviewer.tools.GridAutofitLayoutManager
import com.botty.photoviewer.tools.loadAdWithFailListener
import com.botty.photoviewer.tools.network.Network
import com.botty.photoviewer.tools.startActivity
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : FragmentActivity() {

    private val mainViewModel by lazy { ViewModelProvider(this).get(MainViewModel::class.java) }

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
        adView.loadAdWithFailListener()
        loadGalleries()
    }

    private fun loadGalleries() {
        mainViewModel.galleriesLiveData.observe(this) { galleries ->
            galleriesAdapter.setNewGalleries(galleries)
        }
    }

    private fun onAddNewClick() {
        startActivity<AddShareActivity>()
    }

    private fun onGalleryClick(gallery: Gallery) {
        startActivity<GalleryViewActivity>(Gallery.ID_TAG to gallery.id)
    }

    private fun onSettingsClick() {
        startActivity<SettingsActivity>()
    }

    override fun onDestroy() {
        Network.clean()
        super.onDestroy()
    }
}