package com.botty.photoviewer.dataRepositories.localDB.impl

import com.botty.photoviewer.components.removeLast
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.SimpleItem
import com.botty.photoviewer.data.remoteFolder.FolderContent
import com.botty.photoviewer.dataRepositories.localDB.FoldersRepoDB

class FoldersRepoDBImpl(gallery: Gallery): FoldersRepoDB {
    private var currentFolder = gallery.folder.target
    override var pathTree = mutableListOf(gallery.path)

    override suspend fun loadChildFolders(folderName: String): FolderContent {
        return loadFolder(folderName)
    }

    override suspend fun loadCurrentFolder(): FolderContent {
        return loadFolder()
    }

    override suspend fun loadParentFolder(): FolderContent {
        currentFolder = currentFolder.parentFolder.target
        pathTree.removeLast()
        return loadFolder()
    }

    private fun loadFolder(folderName: String? = null): FolderContent {
        folderName?.let { name ->
            currentFolder.childFolders.forEach folder@{ folder ->
                if(folder.name == name) {
                    currentFolder = folder
                    return@folder
                }
            }
            pathTree.add(currentFolder.name)
        }

        val folders = currentFolder.childFolders.map { folder -> SimpleItem(folder.name)}
        val pictures = currentFolder.childFiles.map { pic -> SimpleItem(pic.name) }
        return FolderContent(folders, pictures, folderPath)
    }
}