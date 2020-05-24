package com.botty.photoviewer.activities.addGallery

import androidx.lifecycle.ViewModel
import com.botty.photoviewer.components.concat
import com.botty.photoviewer.components.isNotNull
import com.botty.photoviewer.components.network.responses.containers.Share
import com.botty.photoviewer.components.removeLast
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.connectionContainers.ConnectionParams
import com.botty.photoviewer.dataRepositories.localDB.ConnectionsRepo
import com.botty.photoviewer.dataRepositories.localDB.GalleriesRepo
import com.botty.photoviewer.dataRepositories.remote.LoginManager
import com.botty.photoviewer.dataRepositories.remote.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject
import org.koin.core.parameter.parametersOf

class AddShareViewModel(connectionsRepo: ConnectionsRepo,
                        private val galleriesRepo: GalleriesRepo
) : ViewModel(), KoinComponent {

    val actualPath = mutableListOf<String>()
    private lateinit var connectionParams: ConnectionParams

    private lateinit var network: Network
    private val loginManager: LoginManager by inject { parametersOf(connectionParams) }

    val connections = connectionsRepo.connections

    //TODO ask if add connection
    suspend fun login(connectionParams: ConnectionParams) = withContext(Dispatchers.IO) {
        this@AddShareViewModel.connectionParams = connectionParams
        loginManager.login().run { network = get{ parametersOf(this)} }
    }

    suspend fun getShares(): List<Share> {
        return network.getShares().shares
    }

    suspend fun loadParentFolder(): List<Share> {
        return if(actualPath.size <= 1) {
            getShares().apply { actualPath.clear() }
        } else {
            loadFolder(null)
        }
    }

    //if folder is null is a parent folder loader request
    suspend fun loadFolder(folder: Share?): List<Share> {
        val path =
            if(folder.isNotNull()) {
                folder!!.path
            } else {
                val pathBuilder = StringBuilder("/")
                for(i in 0 until actualPath.size - 1) {
                    pathBuilder.append("${actualPath[i]}/")
                }
                pathBuilder.dropLast(1).toString()
            }

        return network.getFolders(path).files.run {
            withContext(Dispatchers.Default) {
                filter { share -> share.isNotHidden }
            }
        }.apply {
            //Before to return the folders add this path, or remove if is a parent request
            folder?.run { actualPath.add(this.name) } ?: actualPath.removeLast()
        }
    }

    fun addGalleryToDB(galleryName: String) {
        val path = "/".concat(actualPath)
        val gallery = Gallery(galleryName, path)
        if(connectionParams.id != 0L) {
            gallery.connectionParams.targetId = connectionParams.id
        } else {
            gallery.connectionParams.target = connectionParams
        }
        galleriesRepo.saveGallery(gallery)
    }
}