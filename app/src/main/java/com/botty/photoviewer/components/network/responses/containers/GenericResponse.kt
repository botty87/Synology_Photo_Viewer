package com.botty.photoviewer.components.network.responses.containers

abstract class GenericResponse<T> {
    abstract var success: Boolean
    abstract var error: Error?
    abstract var data: T?

    fun isValid() = success && data != null
}