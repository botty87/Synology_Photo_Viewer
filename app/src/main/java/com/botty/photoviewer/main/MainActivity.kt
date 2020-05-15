package com.botty.photoviewer.main

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.observe
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.main.GalleriesAdapter
import com.botty.photoviewer.addGallery.AddShareActivity
import com.botty.photoviewer.components.GridAutofitLayoutManager
import com.botty.photoviewer.components.loadAdWithFailListener
import com.botty.photoviewer.components.startActivity
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.galleryViewer.galleryView.GalleryViewActivity
import com.botty.photoviewer.settings.SettingsActivity
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
            recyclerViewGalleries.layoutManager = GridAutofitLayoutManager(this@MainActivity, width)
            recyclerViewGalleries.setHasFixedSize(true)
            recyclerViewGalleries.adapter = this
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        adView.loadAdWithFailListener()

        mainViewModel.galleries.observe(this) { galleries ->
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

    /*override fun onDestroy() {
        Network.clean()
        super.onDestroy()
    }*/
}