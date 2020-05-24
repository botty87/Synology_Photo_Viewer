package com.botty.photoviewer.dataRepositories.remote

import com.botty.photoviewer.data.remoteFolder.FolderContent
import com.botty.photoviewer.dataRepositories.FoldersRepo

interface FoldersRepoNet: FoldersRepo {
    suspend fun loadFolderPath(folderPath: String): FolderContent
}