package com.botty.photoviewer.components.network.responses

import com.botty.photoviewer.components.network.responses.containers.Error
import com.botty.photoviewer.components.network.responses.containers.GenericResponse
import com.botty.photoviewer.components.network.responses.containers.Share

data class FoldersResponse(
    override var success: Boolean,
    override var error: Error?,
    override var data: Data?
) : GenericResponse<FoldersResponse.Data>() {
    data class Data(var offset: Int, var files: List<Share>, var total: Int)
}