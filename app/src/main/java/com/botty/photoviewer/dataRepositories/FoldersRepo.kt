package com.botty.photoviewer.dataRepositories

import com.botty.photoviewer.data.remoteFolder.FolderContent

interface FoldersRepo {
    var pathTree: MutableList<String>

    suspend fun loadChildFolders(folderName: String): FolderContent
    suspend fun loadCurrentFolder(): FolderContent
    suspend fun loadParentFolder(): FolderContent

    val folderPath: String
        get() {
            val stringPathBuilder = StringBuilder()
            pathTree.forEach { path ->
                stringPathBuilder.append("$path/")
            }
            return stringPathBuilder.dropLast(1).toString()
        }
}