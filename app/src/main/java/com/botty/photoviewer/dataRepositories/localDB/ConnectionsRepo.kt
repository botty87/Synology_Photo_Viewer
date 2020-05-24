package com.botty.photoviewer.dataRepositories.localDB

import androidx.lifecycle.LiveData
import com.botty.photoviewer.data.connectionContainers.ConnectionParams

interface ConnectionsRepo {
    val connections: LiveData<List<ConnectionParams>>
    fun checkIfConnectionExist(conParams: ConnectionParams): Boolean
}