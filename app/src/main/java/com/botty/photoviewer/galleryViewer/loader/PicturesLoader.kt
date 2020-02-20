package com.botty.photoviewer.galleryViewer.loader

import android.util.SparseArray
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.botty.photoviewer.data.GalleryContainer
import com.botty.photoviewer.data.JobDownloadStatus
import com.botty.photoviewer.data.SessionParams
import com.botty.photoviewer.data.fileStructure.MediaFile
import com.botty.photoviewer.tools.*
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.FutureTarget
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.util.concurrent.ExecutionException

class PicturesLoader private constructor(private val sessionParams: SessionParams,
                                         private val glide: RequestManager,
                                         private val galleryContainer: GalleryContainer,
                                         private val preloadSize: Int) : ViewModel() {

    val pictureNotifier = PictureNotifier()

    /*fun setNewGalleryPath(galleryPath: String) {
        cancelDownload(false)
        this.galleryPath = galleryPath
    }*/

    private var downloadPictureJobs = SparseArray<Job>(preloadSize)
    private var downloadPictureFutureTargets = SparseArray<FutureTarget<File>>(preloadSize)
    private var activeWorkingJob: Job? = null
    @Volatile private var currentDownloadStatus = JobDownloadStatus.NO_WORK

    fun startDownload(currentIndex: Int) {
        startDownload(currentIndex, currentIndex)
    }

    fun startDownload(firstIndex: Int, lastIndex: Int) {
        activeWorkingJob?.cancel()
        if(galleryContainer.hasNoPics) {
            return
        }
        activeWorkingJob = viewModelScope.launch(Dispatchers.Default) {
            Timber.d("$DEBUG_TAG start download")
            currentDownloadStatus = JobDownloadStatus.CURRENT_PIC

            if(downloadPictureJobs.isNotEmpty()) {
                val tempDownloadPictureJobs = SparseArray<Job>(downloadPictureJobs.size())
                for (picIndex in firstIndex..lastIndex) {
                    downloadPictureJobs[picIndex]?.run {
                        tempDownloadPictureJobs.append(picIndex, this)
                        downloadPictureJobs.remove(picIndex)
                    }
                }
                downloadPictureJobs.forEach { job -> job?.cancel() }
                downloadPictureJobs = tempDownloadPictureJobs
            }

            if(downloadPictureFutureTargets.isNotEmpty()) {
                val tempDownloadPictureFutureTargets = SparseArray<FutureTarget<File>>(downloadPictureFutureTargets.size())
                for (picIndex in firstIndex..lastIndex) {
                    downloadPictureFutureTargets[picIndex]?.run {
                        tempDownloadPictureFutureTargets.append(picIndex, this)
                        downloadPictureFutureTargets.remove(picIndex)
                    }
                }
                downloadPictureFutureTargets.forEach { target ->
                    target?.cancel(true)
                    glide.clear(target)
                }
                downloadPictureFutureTargets = tempDownloadPictureFutureTargets
            }

            for (picIndex in firstIndex..lastIndex) {
                if (downloadPictureJobs[picIndex] == null) {
                    Timber.d("$DEBUG_TAG start $picIndex")
                    viewModelScope.launch {
                        downloadAndNotify(picIndex)
                    }.let { job ->
                        downloadPictureJobs.append(picIndex, job)
                        job.invokeOnCompletion {
                            onJobCompletion(picIndex, firstIndex, lastIndex)
                        }
                    }
                } else {
                    Timber.d("$DEBUG_TAG skip $picIndex")
                }
            }
        }
    }

    private suspend fun downloadAndNotify(picIndex: Int) = withContext(Dispatchers.IO) {
        galleryContainer.runCatching {
            if (pictures[picIndex].file?.notExists() != false) {
                val picFullPath =
                    sessionParams.getPicFullPath(path, pictures[picIndex].name)
                glide
                    .asFile()
                    .load(picFullPath)
                    .submit()
                    .apply {
                        downloadPictureFutureTargets.append(picIndex, this)
                    }.get()
                    .let { picFile ->
                        pictures[picIndex].file = picFile
                        pictureNotifier.postValue(picIndex)
                        Timber.d("$DEBUG_TAG pic $picIndex downloaded")
                    }
            }
        }.onFailure { e ->
            if(e is ExecutionException) {
                galleryContainer.pictures[picIndex].run {
                    file = null
                    executionException = true
                }
                pictureNotifier.postValue(picIndex)
            } else {
                e.log()
            }
        }

        /*try {
            if (pictures[picIndex].file?.notExists() != false) {
                val picFullPath =
                    sessionParams.getPicFullPath(galleryPath!!, pictures[picIndex].name)
                glide
                    .asFile()
                    .load(picFullPath)
                    .submit()
                    .apply {
                        downloadPictureFutureTargets.append(picIndex, this)
                    }.get()
                    .let { picFile ->
                        pictures[picIndex].file = picFile
                        pictureNotifier.postValue(picIndex)
                        Timber.d("$DEBUG_TAG pic $picIndex downloaded")
                    }
            }
        } catch (e: NullPointerException) {
            e.log()
        } catch (e: ExecutionException) {
            e.log()
            pictures[picIndex].run {
                file = null
                executionException = true
            }
            pictureNotifier.postValue(picIndex)
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.log()
        }*/
    }

    private fun downloadPreviousPictures(firstIndex: Int, lastIndex: Int) {
        activeWorkingJob = viewModelScope.launch(Dispatchers.Default) {
            if (galleryContainer.hasNoPics || firstIndex == 0) {
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
            galleryContainer.run {
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
    }

    private fun onJobCompletion(jobIndex: Int, firstIndex: Int, lastIndex: Int) {
        downloadPictureJobs.remove(jobIndex)
        glide.clear(downloadPictureFutureTargets[jobIndex])
        downloadPictureFutureTargets.remove(jobIndex)
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

        downloadPictureFutureTargets.forEach { target ->
            target?.cancel(true)
            glide.clear(target)
        }
        downloadPictureFutureTargets.clear()
        downloadPictureJobs.forEach { job -> job?.cancel() }
        downloadPictureJobs.clear()
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val sessionParams: SessionParams,
                  private val glide: RequestManager,
                  private val galleryContainer: GalleryContainer,
                  private val preloadSize: Int): ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PicturesLoader(sessionParams, glide, galleryContainer, preloadSize) as T
        }
    }

    companion object {
        private const val DEBUG_TAG = "ldpic -"
    }
}