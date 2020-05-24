package com.botty.photoviewer.data

import com.botty.photoviewer.data.connectionContainers.ConnectionParams
import com.botty.photoviewer.data.fileStructure.MediaFolder
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToOne
import java.util.*

@Entity
data class Gallery(var name: String, var path: String, @Id var id: Long = 0) {

    //Objectbox constructor
    constructor(lastSync: Date?, name: String, path: String, id: Long = 0, connectionParamsId: Long, folderId: Long) :
            this(name, path, id) {
        connectionParams.targetId = connectionParamsId
        folder.targetId = folderId
        this.lastSync = lastSync
    }

    lateinit var connectionParams: ToOne<ConnectionParams>
    lateinit var folder: ToOne<MediaFolder>

    var lastSync: Date? = null

    override fun equals(other: Any?): Boolean {
        return if(other is Gallery) {
            this.name == other.name &&
            this.path == other.path &&
            this.id == other.id &&
            this.connectionParams.targetId == other.connectionParams.targetId
        } else{
            false
        }
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + connectionParams.hashCode()
        return result
    }

    companion object {
        const val ID_TAG = "gallery_id"
    }
}