package com.botty.photoviewer.components.network.responses.containers

data class Share(var isdir: Boolean, var name: String, var path: String) {
    fun isNotHidden() = !(name.startsWith('.') || name.startsWith('#'))
}