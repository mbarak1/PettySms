package com.example.pettysms.queue

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.example.pettysms.DbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Worker class that handles the synchronization of queue items with EasyQuickImport
 * This worker will check for pending queue items and send them to the server
 */
class QuickBooksWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "QuickBooksWorker"
        private const val SERVER_IP = "api.abdulcon.com" // Update with your actual server IP or domain
        
        // Worker tag for identifying the unique work
        const val WORK_NAME = "quickbooks_sync_worker"
        
        // Track if the worker is currently running
        @Volatile
        private var isWorkerRunning = false
        
        /**
         * Check if worker is running and start it if needed
         * Call this method whenever a new petty cash is added to the queue
         */
        fun startWorkerIfNeeded(context: Context) {
            val workManager = WorkManager.getInstance(context)
            
            // Check current work status
            workManager.getWorkInfosByTag("quickbooks_sync_immediate").get()?.let { workInfos ->
                val isRunning = workInfos.any { !it.state.isFinished }
                if (!isRunning) {
                    // No active work found, schedule immediate sync
                    Log.d(TAG, "No active QuickBooks sync work found, scheduling immediate sync")
                    scheduleImmediateSync(context)
                } else {
                    Log.d(TAG, "QuickBooks sync work is already running")
                }
            }
        }
        
        /**
         * Schedule the worker to run periodically every 5 minutes
         */
        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
                
            val workRequest = PeriodicWorkRequestBuilder<QuickBooksWorker>(
                15, TimeUnit.MINUTES, // Minimum interval allowed by Android
                5, TimeUnit.MINUTES  // Flex interval
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag("quickbooks_sync")
                .setInitialDelay(5, TimeUnit.MINUTES)  // Start first run after 5 minutes
                .keepResultsForAtLeast(1, TimeUnit.HOURS)  // Keep work history
                .build()
                
            WorkManager.getInstance(context).apply {
                // Cancel any existing work
                cancelAllWorkByTag("quickbooks_sync")
                
                // Enqueue new periodic work
                enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE, // Replace any existing work
                    workRequest
                )
            }
                
            Log.d(TAG, "Scheduled periodic QuickBooks sync work every 15 minutes with 5 minute flex")
        }
        
        /**
         * Schedule a one-time immediate sync
         */
        fun scheduleImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
                
            val workRequest = OneTimeWorkRequestBuilder<QuickBooksWorker>()
                .setConstraints(constraints)
                .addTag("quickbooks_sync_immediate")
                .build()
                
            WorkManager.getInstance(context).apply {
                // Cancel any existing immediate sync work
                cancelAllWorkByTag("quickbooks_sync_immediate")
                
                // Enqueue new immediate work
                enqueueUniqueWork(
                    "$WORK_NAME-immediate",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            }
                
            Log.d(TAG, "Scheduled immediate QuickBooks sync work")
        }
        
        /**
         * Initialize the worker at app startup
         * This should be called from the Application class or MainActivity
         */
        fun initializeAtStartup(context: Context) {
            val workManager = WorkManager.getInstance(context)
            
            // First run an immediate sync
            scheduleImmediateSync(context)
            
            // Then schedule the periodic work
            schedulePeriodicWork(context)
            
            // Monitor work status
            workManager.getWorkInfosByTagLiveData("quickbooks_sync").observeForever { workInfos ->
                workInfos?.forEach { workInfo ->
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            Log.d(TAG, "QuickBooks sync work succeeded")
                        }
                        WorkInfo.State.FAILED -> {
                            Log.e(TAG, "QuickBooks sync work failed")
                            // Reschedule after failure
                            schedulePeriodicWork(context)
                        }
                        else -> {
                            Log.d(TAG, "QuickBooks sync work state: ${workInfo.state}")
                        }
                    }
                }
            }
            
            Log.d(TAG, "QuickBooks worker initialized at app startup with monitoring")
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting QuickBooks sync work")
        
        try {
            // Get database helper
            val dbHelper = DbHelper(applicationContext)
            
            // Get all pending queue items
            val pendingItems = dbHelper.getQueueItemsByStatus(QueueItem.STATUS_PENDING)
            
            if (pendingItems.isEmpty()) {
                Log.d(TAG, "No pending items to sync")
                return@withContext Result.success()
            }
            
            Log.d(TAG, "Found ${pendingItems.size} pending items to sync")
            
            // Process each pending item
            var successCount = 0
            var failureCount = 0
            
            for (item in pendingItems) {
                val result = sendItemToEasyQuickImport(item)
                
                if (result.first) {
                    // Update item status to SENT
                    dbHelper.updateQueueItemStatus(item.id!!, QueueItem.STATUS_SENT)
                    successCount++
                    Log.d(TAG, "Successfully synced item ${item.pettyCashNumber}")
                } else {
                    // Update item status to FAILED with error message
                    dbHelper.updateQueueItemStatus(item.id!!, QueueItem.STATUS_FAILED, result.second)
                    failureCount++
                    Log.e(TAG, "Failed to sync item ${item.pettyCashNumber}: ${result.second}")
                }
            }
            
            Log.d(TAG, "Sync completed. Success: $successCount, Failures: $failureCount")
            
            // Notify any active QueueActivity instances to refresh their data
            notifyQueueActivityToRefresh()
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in QuickBooks sync work: ${e.message}", e)
            Result.failure()
        }
    }
    
    /**
     * Send a queue item to EasyQuickImport
     * @return Pair<Boolean, String> where first is success flag and second is error message if any
     */
    private suspend fun sendItemToEasyQuickImport(item: QueueItem): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
                
            val formBody = FormBody.Builder()
                .add("operation", "sendToEasyQuickImport")
                .add("pettyCashNumber", item.pettyCashNumber)
                .add("amount", item.amount.toString())
                .add("description", item.description ?: "")
                .add("date", item.date)
                .add("accountName", item.accountName ?: "")
                .add("ownerName", item.ownerName ?: "")
                
            // Add truck numbers if available
            if (!item.truckNumbers.isNullOrEmpty()) {
                formBody.add("truckNumbers", item.truckNumbers)
            }
                
            val request = Request.Builder()
                .url("https://$SERVER_IP/")
                .post(formBody.build())
                .build()
            
            Log.d(TAG, "Sending item ${item.pettyCashNumber} to EasyQuickImport")
                
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    val jsonResponse = JSONObject(responseBody)
                    
                    if (jsonResponse.optBoolean("success", false)) {
                        val message = jsonResponse.optString("message", "Successfully queued for QuickBooks")
                        Log.d(TAG, "Success response: $message")
                        return@withContext Pair(true, "")
                    } else {
                        val errorMessage = jsonResponse.optString("error", "Unknown error")
                        Log.e(TAG, "Error response: $errorMessage")
                        
                        // Check if this is a duplicate entry error
                        if (errorMessage.contains("already been queued or processed")) {
                            // Mark as sent since it's already in the system
                            Log.d(TAG, "Item ${item.pettyCashNumber} was already processed, marking as sent")
                            return@withContext Pair(true, "Already processed")
                        }
                        
                        return@withContext Pair(false, errorMessage)
                    }
                } else {
                    val errorMsg = "HTTP Error: ${response.code}"
                    Log.e(TAG, errorMsg)
                    return@withContext Pair(false, errorMsg)
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Exception sending item to EasyQuickImport: ${e.message}"
            Log.e(TAG, errorMsg, e)
            return@withContext Pair(false, errorMsg)
        }
    }
    
    /**
     * Notify any active QueueActivity instances to refresh their data
     */
    private fun notifyQueueActivityToRefresh() {
        try {
            // Create the intent with the action
            val intent = Intent(QueueActivity.ACTION_QUEUE_DATA_CHANGED)
            // Make the intent explicit by setting the package name
            intent.setPackage(applicationContext.packageName)
            intent.putExtra("source", "QuickBooksWorker")
            intent.putExtra("timestamp", System.currentTimeMillis())
            
            // Send both local and global broadcasts for maximum compatibility
            // 1. Use LocalBroadcastManager (preferred for in-app communication)
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(applicationContext)
                .sendBroadcast(intent)
            Log.d(TAG, "📤 Sent local broadcast with action: ${QueueActivity.ACTION_QUEUE_DATA_CHANGED}")
            
            // 2. Also send global broadcast for backward compatibility
            applicationContext.sendBroadcast(intent)
            Log.d(TAG, "📤 Sent global broadcast with action: ${QueueActivity.ACTION_QUEUE_DATA_CHANGED} and package: ${applicationContext.packageName}")
            
            // 3. Force a direct call to update the UI if QueueActivity is in foreground
            // This is a fallback mechanism in case broadcasts aren't working
            try {
                val activityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val runningActivities = activityManager.appTasks
                    .flatMap { it.taskInfo.topActivity?.className?.let { listOf(it) } ?: emptyList() }
                
                if (runningActivities.any { it.contains("QueueActivity") }) {
                    Log.d(TAG, "🔍 QueueActivity appears to be in foreground, attempting direct update")
                    // We can't directly call methods on QueueActivity, but we can send a more aggressive broadcast
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    applicationContext.sendBroadcast(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for foreground activity: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending queue data changed broadcast: ${e.message}")
        }
    }
} 