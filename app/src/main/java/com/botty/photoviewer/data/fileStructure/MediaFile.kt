package com.botty.photoviewer.data.fileStructure

import io.objectbox.annotation.Entity
import io.objectbox.relation.ToOne
import java.io.File

@Entity
class MediaFile(
    id: Long = 0,
    name: String,
    galleryId: Long,

    @Transient
    var file: File? = null,
    @Transient
    var timeoutException: Boolean = false
) : BaseMedia(id, name, galleryId) {
    lateinit var folder: ToOne<MediaFolder>

    constructor(id: Long, name: String, galleryId: Long ,folderId: Long) : this(id, name, galleryId) {
        this.folder.targetId = folderId
    }
}