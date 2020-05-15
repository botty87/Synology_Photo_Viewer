package com.botty.photoviewer.components.workers

import android.content.Context
import androidx.work.*
import com.botty.photoviewer.components.log
import com.botty.photoviewer.components.network.Network
import com.botty.photoviewer.data.connectionContainers.ConnectionParams
import com.botty.photoviewer.data.connectionContainers.SessionParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.parameter.parametersOf
import org.koin.ext.getScopeName
import java.util.concurrent.TimeUnit

class LogoutWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params), KoinComponent {

    override suspend fun doWork(): Result {
        val sid = inputData.getString(KEY_SID) ?: return Result.failure()
        val address = inputData.getString(KEY_ADDRESS) ?: return Result.failure()
        val https = inputData.getBoolean(KEY_HTTPS, true)
        val port = inputData.getInt(KEY_PORT, ConnectionParams.DEFAULT_HTTPS_PORT)
        val sessionParams =
            SessionParams(
                address,
                sid,
                port,
                https
            )
        val network: Network by inject(getScopeName()){ parametersOf(sessionParams) }

        return withContext(Dispatchers.IO) {
            fun retry() =
                if(runAttemptCount > MAX_RETRY) {
                    Result.failure()
                } else {
                    Result.retry()
                }

            try {
                if(network.logout()) {
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