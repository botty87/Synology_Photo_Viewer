package com.botty.photoviewer.components.network

import com.botty.photoviewer.data.connectionContainers.SessionParams

interface LoginManager {
    suspend fun login(): SessionParams
}