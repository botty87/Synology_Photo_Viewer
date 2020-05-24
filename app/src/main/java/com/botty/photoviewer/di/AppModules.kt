package com.botty.photoviewer.di

import com.botty.photoviewer.BuildConfig
import com.botty.photoviewer.activities.addGallery.AddShareViewModel
import com.botty.photoviewer.activities.galleryViewer.fullscreenView.FullscreenViewModel
import com.botty.photoviewer.activities.galleryViewer.galleryView.GalleryViewActivity
import com.botty.photoviewer.activities.galleryViewer.galleryView.GalleryViewModel
import com.botty.photoviewer.activities.galleryViewer.loader.GalleryContainer
import com.botty.photoviewer.activities.galleryViewer.loader.PicturesLoader
import com.botty.photoviewer.activities.main.MainViewModel
import com.botty.photoviewer.activities.settings.SettingsActivityViewModel
import com.botty.photoviewer.components.network.*
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.Settings
import com.botty.photoviewer.data.connectionContainers.ConnectionParams
import com.botty.photoviewer.data.connectionContainers.SessionParams
import com.botty.photoviewer.data.db.AppDB
import com.botty.photoviewer.data.db.FoldersRepoDBImpl
import com.botty.photoviewer.data.db.ObjectBox
import com.botty.photoviewer.di.repos.ConnectionsRepo
import com.botty.photoviewer.di.repos.DBFilesRepo
import com.botty.photoviewer.di.repos.DBFoldersRepo
import com.botty.photoviewer.di.repos.GalleriesRepo
import com.bumptech.glide.Glide
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

const val ADS_ID = "ads_id"

val viewModelsModule = module {
    viewModel { MainViewModel(get(), get()) }
    viewModel { AddShareViewModel(get(), get()) }
    viewModel { SettingsActivityViewModel(get(), get()) }
    viewModel { (galleryContainer: GalleryContainer) -> PicturesLoader(get(), galleryContainer) }
    viewModel { (galleryContainer: GalleryContainer) -> FullscreenViewModel(galleryContainer, get()) }
}

val appModule = module {
    single(named(ADS_ID)) {
        if(BuildConfig.DEBUG) {
            "ca-app-pub-3940256099942544~3347511713"
        } else {
            "ca-app-pub-9694877750002081~5509085931"
        }
    }
    single { Glide.with(androidContext()) }
}

val scopedModules = module {
    scope<GalleryViewActivity> {
        scoped { (galleryId: Long) -> get<GalleriesRepo>().getGallery(galleryId) } //Get gallery
        scoped { GalleryContainer()}
        scoped {
            if(get<Settings>().dbMode) {
                FoldersRepoDBImpl(get())
            } else {
                FoldersRepoNetImpl(get<GalleryContainer>(), get())
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
        NetworkImpl(get{ parametersOf(sessionParams.baseUrl) }, sessionParams)
    }

    factory<LoginManager> { (conParams: ConnectionParams) ->
        LoginManagerImpl(get{ parametersOf(conParams.baseUrl) }, conParams) }

    factory<FoldersRepoNet> { (network: Network, gallery: Gallery) ->  FoldersRepoNetImpl(network, gallery) }
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