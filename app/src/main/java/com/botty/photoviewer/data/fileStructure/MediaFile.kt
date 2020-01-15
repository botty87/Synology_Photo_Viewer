package com.botty.photoviewer.data.fileStructure

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.relation.ToOne
import java.io.File

@Entity
data class MediaFile(
    @Id
    var id: Long = 0,
    val name: String,
    @Index
    val galleryId: Long,

    @Transient
    var file: File? = null,
    @Transient
    var executionException: Boolean = false
) {
    lateinit var folder: ToOne<MediaFolder>

    constructor(id: Long, name: String, galleryId: Long ,folderId: Long) : this(id, name, galleryId) {
        this.folder.targetId = folderId
    }
}