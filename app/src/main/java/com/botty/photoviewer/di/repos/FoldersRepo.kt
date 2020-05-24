package com.botty.photoviewer.di.repos

import com.botty.photoviewer.data.remoteFolder.FolderContent

interface FoldersRepo {
    var pathTree: MutableList<String>

    suspend fun loadChildFolders(folderName: String): FolderContent
    suspend fun loadCurrentFolder(): FolderContent
    suspend fun loadParentFolder(): FolderContent
}