package com.zebra.nilac.displaytimeouttoggle

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zebra.nilac.displaytimeouttoggle.utils.EMDKLoader

class DisplayToggleService : Service() {

    private var mIsServiceRunning = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        mIsServiceRunning = true

        EMDKLoader.getInstance()
            .initEMDKManager(this, object : EMDKLoader.EMDKManagerInitCallBacks {
                override fun onSuccess() {
                    Log.i(
                        TAG,
                        "EMDK Manager successfully initialised, proceeding with service start"
                    )

                    val filters: IntentFilter = IntentFilter().apply {
                        addAction(Intent.ACTION_POWER_DISCONNECTED)
                        addAction(Intent.ACTION_POWER_CONNECTED)
                        addAction(STOP_FG_SERVICE_ACTION)
                    }
                    registerReceiver(receiver, filters)

                    startForeground(7171, createServiceNotification())
                }

                override fun onFailed(message: String) {
                    Log.e(
                        TAG,
                        "Failed to initialise the EMDK Manager, can't start the foreground service"
                    )
                }
            })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!mIsServiceRunning) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        EMDKLoader.getInstance().release()
        unregisterReceiver(receiver)
        mIsServiceRunning = false
    }

    private fun createServiceNotification(): Notification {
        val channelId = "com.zebra.displaytimeouttoggle"
        val channelName = "DTS Notification Channel"

        // Create Channel
        val notificationChannel = NotificationChannel(
            channelId, channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        // Set Channel
        val manager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(notificationChannel)

        // Build Notification
        val notificationBuilder = NotificationCompat.Builder(
            this,
            channelId
        )

        // Add action button in the notification
        val stopServicePendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            STOP_FG_SERVICE,
            Intent(STOP_FG_SERVICE_ACTION),
            PendingIntent.FLAG_IMMUTABLE
        )

        // Return Build Notification object
        return notificationBuilder
            .setContentTitle("Service is active")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .addAction(R.drawable.ic_generic_close, "STOP", stopServicePendingIntent)
            .setOngoing(true)
            .build()
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    Log.d(TAG, "Device has been connected to AC Power")
                    updateStayAwakeValue(true)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    Log.d(TAG, "Device is currently running on battery")
                    updateStayAwakeValue(false)
                }
                STOP_FG_SERVICE_ACTION -> {
                    Log.i(TAG, "About to stop the service")
                    stopSelf()
                }
            }
        }
    }

    private fun updateStayAwakeValue(state: Boolean) {
        EMDKLoader.getInstance()
            .processEMDKProfile(
                AppConstants.PROFILE_NAME,
                getUpdatedProfile(state),
                object : EMDKLoader.EMDKProfileResultCallBacks {
                    override fun onProfileLoaded() {
                        Log.d(TAG, "Profile successfully applied")
                    }

                    override fun onProfileLoadFailed(message: String) {
                        Log.d(TAG, "Failed to process the profile")
                    }
                })
    }

    private fun getUpdatedProfile(state: Boolean): String {
        return """
            <wap-provisioningdoc>
                <characteristic type="Profile">
                    <parm name="ProfileName" value="DisplayStayAwakeToggle"/>
                    <characteristic version="4.3" type="DisplayMgr">
                        <parm name="StayAwake" value="${if (state) "1" else "2"}" />
                    </characteristic>
                </characteristic>
            </wap-provisioningdoc>
            """
    }

    companion object {
        const val TAG = "DisplayToggleService"

        const val STOP_FG_SERVICE = 1121
        const val STOP_FG_SERVICE_ACTION =
            "com.zebra.nilac.displaytimeouttoggle.STOP_FG_SERVICE_ACTION"
    }
}