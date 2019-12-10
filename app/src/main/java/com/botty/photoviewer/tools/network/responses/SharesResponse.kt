package com.botty.photoviewer.tools.network.responses

import com.botty.photoviewer.tools.network.responses.containers.Error
import com.botty.photoviewer.tools.network.responses.containers.GenericResponse
import com.botty.photoviewer.tools.network.responses.containers.Share

data class SharesResponse(
    override var success: Boolean,
    override var error: Error?,
    override var data: Data?
) : GenericResponse<SharesResponse.Data>() {
    data class Data(var offset: Int, var shares: List<Share>, var total: Int)
}