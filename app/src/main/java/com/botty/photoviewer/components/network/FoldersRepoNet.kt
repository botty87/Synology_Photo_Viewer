package com.botty.photoviewer.components.network

import com.botty.photoviewer.data.remoteFolder.FolderContent
import com.botty.photoviewer.di.repos.FoldersRepo

interface FoldersRepoNet: FoldersRepo {
    suspend fun loadFolderPath(folderPath: String): FolderContent
}