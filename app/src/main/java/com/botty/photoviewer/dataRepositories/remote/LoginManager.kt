package com.botty.photoviewer.dataRepositories.remote

import com.botty.photoviewer.data.connectionContainers.SessionParams

interface LoginManager {
    suspend fun login(): SessionParams
}