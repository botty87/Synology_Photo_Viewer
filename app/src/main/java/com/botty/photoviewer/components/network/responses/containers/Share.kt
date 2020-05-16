package com.botty.photoviewer.components.network.responses.containers

import com.botty.photoviewer.components.endsWithNoCase
import com.botty.photoviewer.data.remoteFolder.RemoteItem


class Share(var isdir: Boolean, name: String, path: String): RemoteItem(name, path) {
    val isNotHidden: Boolean
        get() = !(name.startsWith('.') || name.startsWith('#'))

    val isPicture: Boolean
        get() = name.endsWithNoCase(".webp") ||
                name.endsWithNoCase(".jpg") ||
                name.endsWithNoCase(".jpeg") ||
                name.endsWithNoCase(".png") ||
                name.endsWithNoCase(".tif") ||
                name.endsWithNoCase(".tiff") ||
                name.endsWithNoCase(".gif")
}