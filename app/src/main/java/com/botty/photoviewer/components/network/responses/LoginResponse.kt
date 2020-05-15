package com.botty.photoviewer.components.network.responses

import com.botty.photoviewer.components.network.responses.containers.Error
import com.botty.photoviewer.components.network.responses.containers.GenericResponse

data class LoginResponse(
    override var success: Boolean,
    override var error: Error?,
    override var data: Data?
) : GenericResponse<LoginResponse.Data>() {
    data class Data(var sid: String)
}