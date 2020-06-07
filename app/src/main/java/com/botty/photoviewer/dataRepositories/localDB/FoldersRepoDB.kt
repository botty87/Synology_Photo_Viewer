package com.botty.photoviewer.dataRepositories.localDB

import com.botty.photoviewer.data.remoteFolder.FolderContent
import com.botty.photoviewer.dataRepositories.FoldersRepo

interface FoldersRepoDB: FoldersRepo {
    val currentFolderId: Long
    suspend fun reloadCurrentFolder(): FolderContent
}