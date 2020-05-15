package com.botty.photoviewer.components.network

import com.botty.photoviewer.data.connectionContainers.ConnectionParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginManagerImpl(private val api: API, private val connectionParams : ConnectionParams): LoginManager, ApiExecuter() {
    override suspend fun login() = withContext(Dispatchers.IO) {
        api.login(connectionParams.user, connectionParams.password).executeApi().run {
            return@withContext connectionParams.toSessionParams(data!!.sid)
        }
    }
}