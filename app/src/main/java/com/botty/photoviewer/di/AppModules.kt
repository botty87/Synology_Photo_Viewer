package com.botty.photoviewer.di

import com.botty.photoviewer.activities.addGallery.AddShareViewModel
import com.botty.photoviewer.activities.galleryViewer.fullscreenView.FullscreenViewModel
import com.botty.photoviewer.activities.galleryViewer.galleryView.GalleryViewActivity
import com.botty.photoviewer.activities.galleryViewer.galleryView.GalleryViewModel
import com.botty.photoviewer.activities.galleryViewer.loader.GalleryContainer
import com.botty.photoviewer.activities.galleryViewer.loader.PicturesLoader
import com.botty.photoviewer.activities.main.MainViewModel
import com.botty.photoviewer.activities.settings.SettingsViewModel
import com.botty.photoviewer.components.network.API
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.connectionContainers.ConnectionParams
import com.botty.photoviewer.data.connectionContainers.SessionParams
import com.botty.photoviewer.dataRepositories.Settings
import com.botty.photoviewer.dataRepositories.localDB.*
import com.botty.photoviewer.dataRepositories.localDB.impl.FoldersRepoDBImpl
import com.botty.photoviewer.dataRepositories.localDB.impl.ObjectBox
import com.botty.photoviewer.dataRepositories.remote.FoldersRepoNet
import com.botty.photoviewer.dataRepositories.remote.LoginManager
import com.botty.photoviewer.dataRepositories.remote.Network
import com.botty.photoviewer.dataRepositories.remote.impl.FoldersRepoNetImpl
import com.botty.photoviewer.dataRepositories.remote.impl.LoginManagerImpl
import com.botty.photoviewer.dataRepositories.remote.impl.NetworkImpl
import com.bumptech.glide.Glide
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val viewModelsModule = module {
    viewModel { MainViewModel(get(), get()) }
    viewModel { AddShareViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { (galleryContainer: GalleryContainer) -> PicturesLoader(get(), galleryContainer) }
    viewModel { (galleryContainer: GalleryContainer) -> FullscreenViewModel(galleryContainer, get()) }
}

val appModule = module {
    single { Glide.with(androidContext()) }
}

val scopedModules = module {
    scope<GalleryViewActivity> {
        scoped { (galleryId: Long) -> get<GalleriesRepo>().getGallery(galleryId) } //Get gallery
        scoped { GalleryContainer()}
        scoped {
            if(get<Settings>().dbMode) {
                FoldersRepoDBImpl(
                    get()
                )
            } else {
                FoldersRepoNetImpl(
                    get<GalleryContainer>(),
                    get()
                )
            }
        }

        this.scoped {
            Glide.with(androidContext())
        }

        viewModel {
            (galleryId: Long) -> GalleryViewModel(get { parametersOf(galleryId) }, get { parametersOf(galleryId) }, get() )
        }

        viewModel{ PicturesLoader(get(), get()) }
    }
}

val networkModule = module {
    factory<API> {(baseUrl : String) ->
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(API::class.java)
    }

    factory<Network> { (sessionParams: SessionParams) ->
        NetworkImpl(get {
            parametersOf(
                sessionParams.baseUrl
            )
        }, sessionParams)
    }

    factory<LoginManager> { (conParams: ConnectionParams) ->
        LoginManagerImpl(get {
            parametersOf(
                conParams.baseUrl
            )
        }, conParams)
    }

    factory<FoldersRepoNet> { (network: Network, gallery: Gallery) ->
        FoldersRepoNetImpl(
            network,
            gallery
        )
    }
}

val dbModule = module {
    single { ObjectBox().apply { init(androidContext()) } }

    single<AppDB> { get<ObjectBox>() }
    single<ConnectionsRepo> { get<AppDB>() }
    single<GalleriesRepo> { get<AppDB>() }
    single<DBFilesRepo> { get<AppDB>() }
    single<DBFoldersRepo> { get<AppDB>() }

    single { Settings() }
}