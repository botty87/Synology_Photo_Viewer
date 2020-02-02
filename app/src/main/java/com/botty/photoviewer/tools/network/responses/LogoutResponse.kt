package com.botty.photoviewer.tools.network.responses

import com.botty.photoviewer.tools.network.responses.containers.Error

data class LogoutResponse(
    val success: Boolean,
    val error: Error? = null)