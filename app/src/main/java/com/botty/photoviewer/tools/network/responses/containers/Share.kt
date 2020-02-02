package com.botty.photoviewer.tools.network.responses.containers

import com.botty.photoviewer.tools.endsWithNoCase

data class Share(var isdir: Boolean, var name: String, var path: String) {
    fun isNotHidden() = !(name.startsWith('.') || name.startsWith('#'))

    fun isPicture(): Boolean = name.endsWithNoCase(".webp") ||
            name.endsWithNoCase(".jpg") ||
            name.endsWithNoCase(".jpeg") ||
            name.endsWithNoCase(".png") ||
            name.endsWithNoCase(".tif") ||
            name.endsWithNoCase(".tiff") ||
            name.endsWithNoCase(".gif")
}