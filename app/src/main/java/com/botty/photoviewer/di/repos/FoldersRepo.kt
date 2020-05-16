package com.botty.photoviewer.di.repos

import com.botty.photoviewer.data.remoteFolder.FolderContent

interface FoldersRepo {
    suspend fun loadFolderContent(path: String) : FolderContent
}