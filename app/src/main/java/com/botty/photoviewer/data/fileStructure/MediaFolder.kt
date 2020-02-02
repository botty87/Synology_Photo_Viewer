package com.botty.photoviewer.data.fileStructure

import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne

@Entity
data class MediaFolder(
    @Id
    var id: Long = 0,
    val name: String,
    @Index
    val galleryId: Long
) {
    lateinit var parentFolder: ToOne<MediaFolder>

    @Backlink(to = "folder")
    lateinit var childFiles: ToMany<MediaFile>

    @Backlink(to = "parentFolder")
    lateinit var childFolders: ToMany<MediaFolder>

    constructor(id: Long, name: String, galleryId: Long, parentFolderId: Long) : this(id, name, galleryId) {
        this.parentFolder.targetId = parentFolderId
    }

}