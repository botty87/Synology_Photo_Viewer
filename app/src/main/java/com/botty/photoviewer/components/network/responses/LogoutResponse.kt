package com.botty.photoviewer.components.network.responses

import com.botty.photoviewer.components.network.responses.containers.Error

data class LogoutResponse(
    val success: Boolean,
    val error: Error? = null)