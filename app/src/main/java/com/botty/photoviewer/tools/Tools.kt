package com.botty.photoviewer.tools

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
object Tools {
    val standardDateParser : SimpleDateFormat
        get() = SimpleDateFormat("dd MMM yyyy - HH:mm:ss").apply {
                timeZone = TimeZone.getTimeZone("UTC")
        }

    /*fun scanGalleries(activity: FragmentActivity, galleryId: Long, mainFolderId: Long = 0, onDone: ((Boolean) -> Unit)? = null) {
        activity.run {
            val workID = ScanGalleriesWorker.setWorker(this, galleryId, mainFolderId)
            WorkManager.getInstance(this).getWorkInfoByIdLiveData(workID)
                .observe(this) { workInfo: WorkInfo? ->
                    when (workInfo?.state) {
                        WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED -> {
                            if(workInfo.state == WorkInfo.State.SUCCEEDED) {
                                onDone?.invoke(true)
                            } else {
                                var errorMessage = workInfo.outputData.getString(ScanGalleriesWorker.ERROR_KEY)
                                errorMessage = "${getString(R.string.scan_error)}: $errorMessage\n${getString(
                                    R.string.retry_scan)}"
                                MaterialDialog(this).show {
                                    title(R.string.error)
                                    message(text = errorMessage)
                                    positiveButton(R.string.yes) {
                                        scanGalleries(activity, galleryId)
                                    }
                                    negativeButton(R.string.no) {
                                        onDone?.invoke(false)
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }*/
}