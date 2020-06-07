package com.botty.photoviewer.data.fileStructure

import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@BaseEntity
abstract class BaseMedia(
    @Id
    var id: Long = 0,
    val name: String,
    @Index
    val galleryId: Long
)