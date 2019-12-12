package com.botty.photoviewer.galleryViewer.loader

import android.graphics.drawable.Drawable
import android.util.SparseArray
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.botty.photoviewer.data.JobDownloadStatus
import com.botty.photoviewer.data.PictureContainer
import com.botty.photoviewer.data.SessionParams
import com.botty.photoviewer.tools.isEmpty
import com.botty.photoviewer.tools.log
import com.botty.photoviewer.tools.notExists
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.FutureTarget
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File

class PicturesLoader private constructor(private val sessionParams: SessionParams,
                                         private val glide: RequestManager,
                                         private val pictures: List<PictureContainer>,
                                         private val preloadSize: Int) : ViewModel() {

    val pictureNotifier = PictureNotifier()

    private var galleryPath: String? = null

    private val debugTag = "ldpic -"

    fun setNewGalleryPath(galleryPath: String) {
        cancelDownload(false)
        this.galleryPath = galleryPath
    }

    private var downloadPictureJobs = SparseArray<Job>(preloadSize)
    private var downloadPictureFutureTarget = SparseArray<FutureTarget<File>>(preloadSize)
    private var activeWorkingJob: Job? = null
    @Volatile private var currentDownloadStatus = JobDownloadStatus.NO_WORK

    fun startDownload(currentIndex: Int) {
        startDownload(currentIndex, currentIndex)
    }

    fun startDownload(firstIndex: Int, lastIndex: Int) {
        activeWorkingJob?.cancel()
        if(pictures.isEmpty()) {
            return
        }
        activeWorkingJob = viewModelScope.launch(Dispatchers.Default) {
            Timber.d("$debugTag start download")
            currentDownloadStatus = JobDownloadStatus.CURRENT_PIC

            for (picIndex in firstIndex..lastIndex) {
                if (downloadPictureJobs[picIndex] == null) {
                    Timber.d("$debugTag start $picIndex")
                    viewModelScope.launch {
                        downloadAndNotify(picIndex)
                    }.let { job ->
                        downloadPictureJobs.append(picIndex, job)
                        job.invokeOnCompletion {
                            onJobCompletion(picIndex, firstIndex, lastIndex)
                        }
                    }
                } else {
                    Timber.d("$debugTag skip $picIndex")
                }
            }
        }
    }

    private suspend fun downloadAndNotify(picIndex: Int) = withContext(Dispatchers.IO) {
        try {
            if (pictures[picIndex].file?.notExists() != false) {
                val picFullPath =
                    sessionParams.getPicFullPath(galleryPath!!, pictures[picIndex].name)

                /*glide
                    .asFile()
                    .load(picFullPath)
                    .into(object : CustomTarget<File>() {
                        override fun onLoadCleared(placeholder: Drawable?) {
                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }

                        override fun onResourceReady(
                            resource: File,
                            transition: Transition<in File>?
                        ) {
                            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                        }

                    })*/

                //TODO test
                delay(5000)
                glide
                    .asFile()
                    .load(picFullPath)
                    .submit()
                    .apply {
                        downloadPictureFutureTarget.append(picIndex, this)
                    }.get()
                    .let { picFile ->
                        pictures[picIndex].file = picFile
                        pictureNotifier.postValue(picIndex)
                        Timber.d("$debugTag pic $picIndex downloaded")
                    }
            }
        } catch (e: NullPointerException) {
            e.log()
        }
    }

    private fun downloadPreviousPictures(firstIndex: Int, lastIndex: Int) {
        activeWorkingJob = viewModelScope.launch(Dispatchers.Default) {
            if (pictures.isEmpty() || firstIndex == 0) {
                currentDownloadStatus = JobDownloadStatus.NO_WORK
                return@launch
            }
            currentDownloadStatus = JobDownloadStatus.PREV_PIC

            val startPosition = firstIndex - 1
            var endPos = firstIndex - preloadSize
            if (endPos < 0) {
                endPos = 0
            }

            for (picIndex in startPosition downTo endPos) {
                if (downloadPictureJobs[picIndex] == null) {
                    viewModelScope.launch {
                        downloadAndNotify(picIndex)
                    }.let { job ->
                        downloadPictureJobs.append(picIndex, job)
                        job.invokeOnCompletion {
                            onJobCompletion(picIndex, firstIndex, lastIndex)
                        }
                    }
                }
            }
        }
    }

    private fun downloadNextPictures(firstIndex: Int, lastIndex: Int) {
        activeWorkingJob = viewModelScope.launch(Dispatchers.Default) {
            currentDownloadStatus = JobDownloadStatus.NEXT_PIC
            when{
                pictures.isEmpty() -> {
                    currentDownloadStatus = JobDownloadStatus.NO_WORK
                    return@launch
                }

                lastIndex == pictures.size - 1 -> {
                    downloadPreviousPictures(firstIndex, lastIndex)
                    return@launch
                }
            }

            val startPosition = lastIndex + 1
            var endPos = lastIndex + preloadSize
            if (endPos >= pictures.size) {
                endPos = pictures.size - 1
            }

            for (picIndex in startPosition..endPos) {
                if (downloadPictureJobs[picIndex] == null) {
                    viewModelScope.launch {
                        downloadAndNotify(picIndex)
                    }.let { job ->
                        downloadPictureJobs.append(picIndex, job)
                        job.invokeOnCompletion {
                            onJobCompletion(picIndex, firstIndex, lastIndex)
                        }
                    }
                }
            }
        }
    }

    private fun onJobCompletion(jobIndex: Int, firstIndex: Int, lastIndex: Int) {
        downloadPictureJobs.remove(jobIndex)
        glide.clear(downloadPictureFutureTarget[jobIndex])
        downloadPictureFutureTarget.remove(jobIndex)
        if(downloadPictureJobs.isEmpty()) {
            when(currentDownloadStatus) {
                JobDownloadStatus.CURRENT_PIC -> {
                    downloadNextPictures(firstIndex, lastIndex)
                }
                JobDownloadStatus.NEXT_PIC -> {
                    downloadPreviousPictures(firstIndex, lastIndex)
                }
                else -> {
                    activeWorkingJob = null
                    currentDownloadStatus = JobDownloadStatus.NO_WORK
                }
            }
        }
    }

    fun cancelDownload(clearAll: Boolean) {
        currentDownloadStatus = JobDownloadStatus.NO_WORK
        activeWorkingJob?.cancel()
        activeWorkingJob = null
        if(clearAll) {
            galleryPath = null
        }
        for(i in 0 until downloadPictureJobs.size()) {
            downloadPictureJobs.valueAt(i)?.cancel()
        }
        downloadPictureJobs.clear()
    }

    class Factory(private val sessionParams: SessionParams,
                  private val glide: RequestManager,
                  private val pictures: List<PictureContainer>,
                  private val preloadSize: Int): ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PicturesLoader(sessionParams, glide, pictures, preloadSize) as T
        }
    }
}