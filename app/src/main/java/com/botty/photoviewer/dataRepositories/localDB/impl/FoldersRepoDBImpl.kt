package com.botty.photoviewer.dataRepositories.localDB.impl

import com.botty.photoviewer.components.removeLast
import com.botty.photoviewer.components.workers.GalleryFolderScanWorker
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.SimpleItem
import com.botty.photoviewer.data.fileStructure.MediaFolder
import com.botty.photoviewer.data.remoteFolder.FolderContent
import com.botty.photoviewer.dataRepositories.localDB.DBFoldersRepo
import com.botty.photoviewer.dataRepositories.localDB.FoldersRepoDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.get

class FoldersRepoDBImpl(gallery: Gallery): FoldersRepoDB, KoinComponent {
    private var currentFolder: MediaFolder = gallery.folder.target
    override var pathTree: MutableList<String>? = mutableListOf(gallery.path)

    override val currentFolderId: Long
        get() = currentFolder.id

    override suspend fun loadChildFolders(folderName: String): FolderContent {
        return loadFolder(folderName)
    }

    override suspend fun loadCurrentFolder(): FolderContent {
        return loadFolder()
    }

    override suspend fun loadParentFolder(): FolderContent {
        currentFolder = currentFolder.parentFolder.target
        pathTree!!.removeLast()
        return loadFolder()
    }

    override suspend fun reloadCurrentFolder(): FolderContent = withContext(Dispatchers.IO) {
        val dbFoldersRepo: DBFoldersRepo = get()
        currentFolder = dbFoldersRepo.getFolder(currentFolderId)
        loadFolder(scanForUpdate = false)
    }

    private suspend fun loadFolder(folderName: String? = null, scanForUpdate: Boolean = true): FolderContent = withContext(Dispatchers.Default) {
        folderName?.let { name ->
            currentFolder.childFolders.forEach folder@{ folder ->
                if(folder.name == name) {
                    currentFolder = folder
                    return@folder
                }
            }
            pathTree!!.add(currentFolder.name)
        }

        val folders = currentFolder.childFolders.map { folder -> SimpleItem(folder.name)}
        val pictures = currentFolder.childFiles.map { pic -> SimpleItem(pic.name) }
        FolderContent(folders, pictures, folderPath).apply {
            if(scanForUpdate) {
                GalleryFolderScanWorker.setWorker(get(), currentFolder.id)
            }
        }
    }
}