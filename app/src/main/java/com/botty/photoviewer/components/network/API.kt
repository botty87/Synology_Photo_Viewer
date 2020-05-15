package com.botty.photoviewer.components.network

import com.botty.photoviewer.components.network.responses.FoldersResponse
import com.botty.photoviewer.components.network.responses.LoginResponse
import com.botty.photoviewer.components.network.responses.LogoutResponse
import com.botty.photoviewer.components.network.responses.SharesResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

private const val SESSION_NAME = "PhotoViewer"

private const val LOGIN_URL = "auth.cgi?api=SYNO.API.Auth&version=3&method=login&session=${SESSION_NAME}&format=sid"
private const val LOGOUT_URL = "auth.cgi?api=SYNO.API.Auth&version=1&method=logout&session=${SESSION_NAME}"
private const val SHARES_URL = "entry.cgi?api=SYNO.FileStation.List&version=2&method=list_share&sort_by=name"
private const val FOLDERS_URL = "entry.cgi?api=SYNO.FileStation.List&version=2&method=list&sort_by=name&filetype=dir"
private const val FOLDERS_CONTENT_URL = "entry.cgi?api=SYNO.FileStation.List&version=2&method=list&sort_by=name"
internal const val DOWNLOAD_FILE_URL = "entry.cgi?api=SYNO.FileStation.Download&version=2&method=download&mode=download"

interface API {
    @GET(LOGIN_URL)
    fun login(@Query("account") username: String, @Query("passwd") password: String): Call<LoginResponse>

    @GET(LOGOUT_URL)
    fun logout(@Query("_sid") sid: String): Call<LogoutResponse>

    @GET(SHARES_URL)
    fun getShares(@Query("_sid") sid: String): Call<SharesResponse>

    @GET(FOLDERS_URL)
    fun getFolders(@Query("folder_path") folderPath: String, @Query("_sid") sid: String): Call<FoldersResponse>

    @GET(FOLDERS_CONTENT_URL)
    fun getFoldersContent(@Query("folder_path") folderPath: String, @Query("_sid") sid: String): Call<FoldersResponse>
}