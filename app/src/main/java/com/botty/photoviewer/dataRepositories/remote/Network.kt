package com.botty.photoviewer.dataRepositories.remote

import com.botty.photoviewer.components.network.responses.FoldersResponse
import com.botty.photoviewer.components.network.responses.SharesResponse

interface Network {
    suspend fun logout() : Boolean
    suspend fun getShares() : SharesResponse.Data
    suspend fun getFolders(folderPath: String): FoldersResponse.Data
    suspend fun getFoldersContent(folderPath: String): FoldersResponse.Data
    fun getPictureUrl(pictureGalleryPath: String, picName: String): String
}