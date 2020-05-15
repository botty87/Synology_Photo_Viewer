package com.botty.photoviewer.components.network

import com.botty.photoviewer.MyApplication
import com.botty.photoviewer.R
import com.botty.photoviewer.components.network.responses.FoldersResponse
import com.botty.photoviewer.components.network.responses.SharesResponse
import com.botty.photoviewer.data.connectionContainers.SessionParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

//with connParams we can do all. If we already have a sessionParams it is fine anyway, except login
class NetworkImpl(private val api: API, private val sessionParams: SessionParams): Network, ApiExecuter() {

    override suspend fun logout() : Boolean = withContext(Dispatchers.IO) {
        api.logout(sessionParams.sid).executeApi().success
    }

    override suspend fun getShares(): SharesResponse.Data = withContext(Dispatchers.IO) {
        api.getShares(sessionParams.sid).executeApi().data!!
    }

    override suspend fun getFolders(folderPath: String): FoldersResponse.Data = withContext(Dispatchers.IO) {
        api.getFolders(folderPath, sessionParams.sid).executeApi().data!!
    }

    override suspend fun getFoldersContent(folderPath: String): FoldersResponse.Data = withContext(Dispatchers.IO) {
        api.getFoldersContent(folderPath, sessionParams.sid).executeApi().data!!
    }

    override fun getPictureUrl(pictureGalleryPath: String, picName: String): String {
        val fullPicPath = "$pictureGalleryPath/$picName"
        return "${sessionParams.baseUrl}$DOWNLOAD_FILE_URL&path=$fullPicPath&_sid=${sessionParams.sid}"
    }

    class NoSuccessResponse: Exception(MyApplication.getString(R.string.no_success_response))
    class NewLoginException: Exception(MyApplication.getString(R.string.new_login_required))
}