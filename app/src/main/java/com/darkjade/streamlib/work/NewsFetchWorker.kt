package com.darkjade.streamlib.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.darkjade.streamlib.data.repository.NewsRepository

/**
 * Periodically refreshes the News feed in the background so it's already
 * up to date when the user opens News — no manual refresh required every
 * time, and this never runs more often than the periodic interval, so it
 * doesn't hammer the network.
 */
class NewsFetchWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        const val UNIQUE_WORK_NAME = "news_periodic_fetch"
    }

    override suspend fun doWork(): Result {
        return try {
            NewsRepository(applicationContext).refreshAll()
            Result.success()
        } catch (e: Exception) {
            // Never crash — just retry on the next scheduled run.
            Result.retry()
        }
    }
}
