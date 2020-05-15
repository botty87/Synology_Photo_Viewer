package com.botty.photoviewer.components.network

import com.botty.photoviewer.components.network.responses.LogoutResponse
import com.botty.photoviewer.components.network.responses.containers.GenericResponse
import okhttp3.ResponseBody
import retrofit2.Call
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

abstract class ApiExecuter {
    //This function execute the api and automatically serves the right response
    suspend fun <T> Call<T>.executeApi() = suspendCoroutine<T> { continuation ->
        fun Continuation<T>.resumeWithResponse(response: T) {
            when(response) {
                is GenericResponse<*> -> {
                    when {
                        response.isValid() -> resume(response)
                        response.error?.newLoginNeeded == true -> resumeWithException(NetworkImpl.NewLoginException())
                        response.error != null -> resumeWithException(Exception(response.error!!.message))
                        else -> resumeWithException(NetworkImpl.NoSuccessResponse())
                    }
                }

                is LogoutResponse -> {
                    if(response.success) {
                        resume(response)
                    } else {
                        resumeWithException(NetworkImpl.NoSuccessResponse())
                    }
                }

                is ResponseBody -> resume(response)

                else -> resumeWithException(NetworkImpl.NoSuccessResponse())
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
                continuation.resumeWithException(NetworkImpl.NoSuccessResponse())
            }
        }
    }
}