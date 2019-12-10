package com.botty.photoviewer.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
class ConnectionParams(val address: String,
                       val user: String,
                       var password: String,
                       val port: Int,
                       val https: Boolean,
                       @Id var id: Long = 0) {
    override fun equals(other: Any?): Boolean {
        return if(other is ConnectionParams) {
            this.address == other.address &&
            this.user == other.user &&
            this.password == other.password &&
            this.port == other.port &&
            this.https == other.https &&
            this.id == other.id
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + user.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + port.hashCode()
        result = 31 * result + https.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

    fun toSessionParams(sid: String): SessionParams = SessionParams(address, sid, port, https, id)

    companion object {
        const val DEFAULT_HTTP_PORT = 5000
        const val DEFAULT_HTTPS_PORT = 5001
    }
}