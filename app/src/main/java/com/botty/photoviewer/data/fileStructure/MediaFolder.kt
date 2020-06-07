package com.botty.photoviewer.data.fileStructure

import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Entity
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne

@Entity
class MediaFolder(id: Long = 0L, name: String, galleryId: Long) :
    BaseMedia(id, name, galleryId) {

    lateinit var parentFolder: ToOne<MediaFolder>

    @Backlink(to = "folder")
    lateinit var childFiles: ToMany<MediaFile>

    @Backlink(to = "parentFolder")
    lateinit var childFolders: ToMany<MediaFolder>

    constructor(id: Long, name: String, galleryId: Long, parentFolderId: Long) : this(id, name, galleryId) {
        this.parentFolder.targetId = parentFolderId
    }

}