package com.botty.photoviewer.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToOne
import java.util.*

@Entity
data class Gallery(var name: String, var path: String,
              var headerToSet: Boolean = true, var lastSync: Date? = null, @Id var id: Long = 0) {

    constructor(name: String, path: String, headerToSet: Boolean = true, id: Long = 0,
                lastSync:Date? = null, connectionParamsId: Long) :
            this(name, path, headerToSet, lastSync, id) {
        connectionParams.targetId = connectionParamsId
        this.lastSync = lastSync
    }

    lateinit var connectionParams: ToOne<ConnectionParams>
    //lateinit var mainFolder: ToOne<MediaFolder>

    override fun equals(other: Any?): Boolean {
        return if(other is Gallery) {
            this.name == other.name &&
            this.path == other.path &&
            this.headerToSet == other.headerToSet &&
            this.id == other.id &&
            this.connectionParams.targetId == other.connectionParams.targetId
        } else{
            false
        }
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + headerToSet.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + connectionParams.hashCode()
        return result
    }

    companion object {
        const val ID_TAG = "gallery_id"
    }
}