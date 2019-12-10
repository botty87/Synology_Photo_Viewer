package com.botty.photoviewer.tools.network.responses.containers

abstract class GenericResponse<T> {
    abstract var success: Boolean
    abstract var error: Error?
    abstract var data: T?

    fun isValid() = success && data != null
}