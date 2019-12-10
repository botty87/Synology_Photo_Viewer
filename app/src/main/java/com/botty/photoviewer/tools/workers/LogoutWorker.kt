package com.botty.photoviewer.tools.workers

import android.content.Context
import androidx.work.*
import com.botty.photoviewer.data.ConnectionParams
import com.botty.photoviewer.data.SessionParams
import com.botty.photoviewer.tools.log
import com.botty.photoviewer.tools.network.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class LogoutWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val sid = inputData.getString(KEY_SID) ?: return Result.failure()
        val address = inputData.getString(KEY_ADDRESS) ?: return Result.failure()
        val https = inputData.getBoolean(KEY_HTTPS, true)
        val port = inputData.getInt(KEY_PORT, ConnectionParams.DEFAULT_HTTPS_PORT)
        val sessionParams = SessionParams(address, sid, port, https)

        return withContext(Dispatchers.IO) {
            fun retry() =
                if(runAttemptCount > MAX_RETRY) {
                    Result.failure()
                } else {
                    Result.retry()
                }

            try {
                if(Network.logout(sessionParams)) {
                    Result.success()
                } else {
                    retry()
                }
            } catch (e: Exception) {
                e.log()
                retry()
            }
        }
    }

    companion object {
        private const val TAG = "logout_job"
        private const val KEY_SID = "sid"
        private const val KEY_ADDRESS = "address"
        private const val KEY_HTTPS = "https"
        private const val KEY_PORT = "port"
        private const val MAX_RETRY = 5

        fun setWorker(context: Context, sessionParams: SessionParams) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = workDataOf(KEY_SID to sessionParams.sid,
                KEY_ADDRESS to sessionParams.address,
                KEY_HTTPS to sessionParams.https,
                KEY_PORT to sessionParams.port)

            val work = OneTimeWorkRequestBuilder<LogoutWorker>()
                .addTag(TAG)
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork("$TAG-${sessionParams.sid}", ExistingWorkPolicy.REPLACE, work)
        }
    }
}