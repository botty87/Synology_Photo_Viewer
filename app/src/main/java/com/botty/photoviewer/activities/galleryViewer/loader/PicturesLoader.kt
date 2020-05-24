package com.botty.photoviewer.activities.galleryViewer.loader

import android.os.Handler
import android.util.SparseArray
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.botty.photoviewer.components.*
import com.botty.photoviewer.data.JobDownloadStatus
import com.botty.photoviewer.data.remoteFolder.PictureContainer
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.FutureTarget
import kotlinx.coroutines.*
import org.koin.core.KoinComponent
import timber.log.Timber
import java.io.File
import java.util.concurrent.ExecutionException


private const val preloadSize: Int = 10

class PicturesLoader (private val glide: RequestManager,
                      private val gallCont: GalleryContainer) : ViewModel(), KoinComponent {

    private val pictures: List<PictureContainer>
        get() = gallCont.pictures.value!!

    private var downloadPicturesHandler: Handler? = null
    val pictureNotifier = PictureNotifier()

    private val debugTag = "ldpic -"

    private var downloadPictureJobs = SparseArray<Job>(preloadSize)
    private var downloadPictureFutureTargets = SparseArray<FutureTarget<File>>(preloadSize)
    private var activeWorkingJob: Job? = null
    @Volatile private var currentDownloadStatus = JobDownloadStatus.NO_WORK

    /*fun startDownload(currentIndex: Int) {
        startDownload(currentIndex, currentIndex)
    }*/

    fun startDownload(firstIndex: Int, lastIndex: Int = firstIndex, withDelay: Boolean = false) {
        downloadPicturesHandler?.removeCallbacksAndMessages(null)
        downloadPicturesHandler = null

        val runnable = Runnable {
            startDownload(firstIndex, lastIndex)
            downloadPicturesHandler = null
        }

        if(withDelay) {
            downloadPicturesHandler = Handler().apply {
                postDelayed( runnable,700)
            }
        } else {
            runnable.run()
        }
    }

    private fun startDownload(firstIndex: Int, lastIndex: Int) {
        activeWorkingJob?.cancel()
        if(pictures.isEmpty()) {
            return
        }
        activeWorkingJob = viewModelScope.launch(Dispatchers.Default) {
            Timber.d("$debugTag start download")
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
        if(picIndex < 0) {
            return@withContext
        }
        try {
            if (pictures[picIndex].file?.notExists() != false) {
                val picFullPath =
                    gallCont.network.getPictureUrl(gallCont.currentGalleryPath, pictures[picIndex].name)
                glide
                    .asFile()
                    .load(picFullPath)
                    .submit()
                    .apply {
                        downloadPictureFutureTargets.append(picIndex, this)
                    }.get()
                    .let { picFile ->
                        try {
                            pictures[picIndex].file = picFile
                            pictureNotifier.postValue(picIndex)
                            Timber.d("$debugTag pic $picIndex downloaded")
                        } catch(e: IndexOutOfBoundsException) {
                            e.log()
                        }
                    }
            }
        } catch(e: IndexOutOfBoundsException) {
            e.log()
        } catch (e: NullPointerException) {
            e.log()
        } catch (e: ExecutionException) {
            e.log()
            pictures[picIndex].run {
                file = null
                timeoutException = true
            }
            pictureNotifier.postValue(picIndex)
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

    fun cancelDownload() {
        currentDownloadStatus = JobDownloadStatus.NO_WORK
        activeWorkingJob?.cancel()
        activeWorkingJob = null

        downloadPictureFutureTargets.forEach { target ->
            target?.cancel(true)
            glide.clear(target)
        }
        downloadPictureFutureTargets.clear()
        downloadPictureJobs.forEach { job -> job?.cancel() }
        downloadPictureJobs.clear()
    }
}