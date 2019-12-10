package com.botty.photoviewer.tools.network

import android.util.SparseArray
import com.botty.photoviewer.MyApplication
import com.botty.photoviewer.R
import com.botty.photoviewer.data.ConnectionParams
import com.botty.photoviewer.data.SessionParams
import com.botty.photoviewer.tools.network.responses.FoldersResponse
import com.botty.photoviewer.tools.network.responses.LoginResponse
import com.botty.photoviewer.tools.network.responses.LogoutResponse
import com.botty.photoviewer.tools.network.responses.SharesResponse
import com.botty.photoviewer.tools.network.responses.containers.GenericResponse
import com.botty.photoviewer.tools.set
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object Network {
    private val apiContainer by lazy { SparseArray<API>() }

    private fun SessionParams.getBaseUrl(): String {
        val protocol = if(https) { "https" } else { "http" }
        return "$protocol://$address:$port/webapi/"
    }

    private fun ConnectionParams.getApi() = this.toSessionParams("").getApi()

    private fun SessionParams.getApi(): API {
        fun createAndStoreApi(): API =
            Retrofit.Builder()
                .baseUrl(getBaseUrl())
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(API::class.java)
                .apply {
                    if(connectionId > 0) {
                        apiContainer[connectionId.toInt()] = this
                    }
                }

        return if(connectionId > 0) {
            apiContainer[connectionId.toInt(), null] ?: createAndStoreApi()
        } else {
            createAndStoreApi()
        }
    }

    fun clean() {
        apiContainer.clear()
    }

    private const val SESSION_NAME = "PhotoViewer"

    private const val LOGIN_URL = "auth.cgi?api=SYNO.API.Auth&version=3&method=login&session=$SESSION_NAME&format=sid"
    private const val LOGOUT_URL = "auth.cgi?api=SYNO.API.Auth&version=1&method=logout&session=$SESSION_NAME"
    private const val SHARES_URL = "entry.cgi?api=SYNO.FileStation.List&version=2&method=list_share&sort_by=name"
    private const val FOLDERS_URL = "entry.cgi?api=SYNO.FileStation.List&version=2&method=list&sort_by=name&filetype=dir"
    private const val FOLDERS_CONTENT_URL = "entry.cgi?api=SYNO.FileStation.List&version=2&method=list&sort_by=name"
    private const val DOWNLOAD_FILE_URL = "entry.cgi?api=SYNO.FileStation.Download&version=2&method=download&mode=download"

    private interface API {
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

    private suspend fun <T> Call<T>.executeApi() = suspendCoroutine<T> { continuation ->
        fun Continuation<T>.resumeWithResponse(response: T) {
            when(response) {
                is GenericResponse<*> -> {
                    when {
                        response.isValid() -> resume(response)
                        response.error?.newLoginNeeded == true -> resumeWithException(NewLoginException())
                        response.error != null -> resumeWithException(Exception(response.error!!.message))
                        else -> resumeWithException(NoSuccessResponse())
                    }
                }

                is LogoutResponse -> {
                    if(response.success) {
                        resume(response)
                    } else {
                        resumeWithException(NoSuccessResponse())
                    }
                }

                is ResponseBody -> resume(response)

                else -> resumeWithException(NoSuccessResponse())
            }
        }

        runCatching {
            execute()
        }.onFailure { e ->
            continuation.resumeWithException(e)
        }.onSuccess { response ->
            if(response.isSuccessful && response.body() != null) {
                continuation.resumeWithResponse(response.body()!!)
            } else {
                continuation.resumeWithException(NoSuccessResponse())
            }
        }
    }

    suspend fun login(connectionParams: ConnectionParams) = withContext(Dispatchers.IO) {
        connectionParams.run {
            getApi().login(user, password).executeApi().data!!
        }
    }

    suspend fun logout(sessionParams: SessionParams) = withContext(Dispatchers.IO) {
        sessionParams.run {
            getApi().logout(sid).executeApi().success
        }
    }

    suspend fun getShares(sessionParams: SessionParams) = withContext(Dispatchers.IO) {
        sessionParams.run {
            try {
                getApi().getShares(sid).executeApi().data!!
            } catch (e: NewSidException) {
                getApi().getShares(e.sid).executeApi().data!!
            }
        }
    }

    suspend fun getFolders(sessionParams: SessionParams, folderPath: String) = withContext(Dispatchers.IO) {
        sessionParams.run {
            try {
                getApi().getFolders(folderPath, sid).executeApi().data!!
            } catch (e: NewSidException) {
                getApi().getFolders(folderPath, e.sid).executeApi().data!!
            }
        }
    }

    suspend fun getFoldersContent(sessionParams: SessionParams, folderPath: String) = withContext(Dispatchers.IO) {
        sessionParams.run {
            try {
                getApi().getFoldersContent(folderPath, sid).executeApi().data!!
            } catch (e: NewSidException) {
                getApi().getFoldersContent(folderPath, e.sid).executeApi().data!!
            }
        }
    }

    fun getPictureUrl(sessionParams: SessionParams, picPath: String) =
        sessionParams.run {
            "${getBaseUrl()}$DOWNLOAD_FILE_URL&path=$picPath&_sid=${sid}"
        }

    class NoSuccessResponse: Exception(MyApplication.getString(R.string.no_success_response))
    class NewLoginException: Exception(MyApplication.getString(R.string.new_login_required))
    class NewSidException(val sid: String): Exception()
}