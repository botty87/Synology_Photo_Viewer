package com.botty.photoviewer.data.remoteFolder

import com.botty.photoviewer.data.SimpleItem

data class FolderContent(val folders: List<SimpleItem>, val pictures: List<SimpleItem>, val folderPath: String)