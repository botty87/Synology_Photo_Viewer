package com.botty.photoviewer.galleryViewer.loader

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import java.util.*

class PictureNotifier : MutableLiveData<Int>() {

    private val queuedValues = LinkedList<Int>()

    @Synchronized
    override fun postValue(value: Int) {
        // We queue the value to ensure it is delivered
        // even if several ones are posted right after.
        // Then we call the base, which will eventually
        // call setValue().
        queuedValues.offer(value)
        super.postValue(value)
    }

    @MainThread
    @Synchronized
    override fun setValue(value: Int) {
        // We first try to remove the value from the queue just
        // in case this line was reached from postValue(),
        // otherwise we will have it duplicated in the queue.
        queuedValues.remove(value)

        // We queue the new value and finally deliver the
        // entire queue of values to the observers.
        queuedValues.offer(value)
        while (!queuedValues.isEmpty())
            super.setValue(queuedValues.poll())
    }
}