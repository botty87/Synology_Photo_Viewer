package com.botty.photoviewer.activities.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.di.repos.GalleriesRepo


class MainViewModel(galleriesRepo: GalleriesRepo) : ViewModel() {
    val galleries: LiveData<List<Gallery>> = galleriesRepo.galleries
}