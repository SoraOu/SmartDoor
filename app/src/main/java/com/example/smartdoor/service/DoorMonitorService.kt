package com.example.smartdoor.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.smartdoor.R
import com.example.smartdoor.utils.NotificationHelper
import com.google.firebase.database.*
import org.json.JSONArray
import com.example.smartdoor.MainActivity


class DoorMonitorService : Service() {

    private val database = FirebaseDatabase.getInstance(
        "https://door--smart-security-default-rtdb.asia-southeast1.firebasedatabase.app"
    )
    private val notifiedKeys = mutableSetOf<String>()
    private var isListenerActive = false

    override fun onCreate() {
        super.onCreate()
        try {
            NotificationHelper(this).createNotificationChannel()
            createNotificationChannel()
            loadNotifiedKeys()
            Log.d("DoorMonitorService", "Service created and initialized successfully")
        } catch (e: Exception) {
            Log.e("DoorMonitorService", "Error in onCreate: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForegroundService()
            if (!isListenerActive) {
                listenForDoorEvents()
                isListenerActive = true
            }
        } catch (e: Exception) {
            Log.e("DoorMonitorService", "Error in onStartCommand: ${e.message}")
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        try {
            val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
                .setContentTitle("Smart Door Monitoring")
                .setContentText("Listening for door activity...")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .build()

            startForeground(1, notification)
        } catch (e: Exception) {
            Log.e("DoorMonitorService", "Failed to start foreground service: ${e.message}")
        }
    }

    private fun listenForDoorEvents() {
        val historyRef = database.getReference("history")
        Log.d("DoorMonitorService", "Listening to 'history' node in Firebase")

        historyRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key ?: return

                val date = snapshot.child("entry_date").value?.toString()
                val time = snapshot.child("entry_time").value?.toString()
                val imageUrl = snapshot.child("entry_img_url").value?.toString()

                Log.d("DoorMonitorService", "Parsed data - Date: $date, Time: $time, Image: $imageUrl")

                // Don't proceed if data is incomplete
                if (date.isNullOrEmpty() || time.isNullOrEmpty() || imageUrl.isNullOrEmpty()) {
                    Log.w("DoorMonitorService", "Data incomplete for key: $key — skipping for now")
                    return
                }

                if (notifiedKeys.contains(key)) return

                notifiedKeys.add(key)
                saveNotifiedKeys()
                sendNotification("Door Activity Detected", date, time, imageUrl)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key ?: return
                if (notifiedKeys.contains(key)) return

                val date = snapshot.child("entry_date").value?.toString()
                val time = snapshot.child("entry_time").value?.toString()
                val imageUrl = snapshot.child("entry_img_url").value?.toString()

                if (!date.isNullOrEmpty() && !time.isNullOrEmpty() && !imageUrl.isNullOrEmpty()) {
                    Log.d("DoorMonitorService", "Recovered complete data in onChildChanged for $key")
                    notifiedKeys.add(key)
                    saveNotifiedKeys()
                    sendNotification("Door Activity Detected", date, time, imageUrl)
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("DoorMonitorService", "Firebase cancelled: ${error.message}")
            }
        })
    }

    private fun sendNotification(title: String, date: String?, time: String?, imageUrl: String?) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "door_activity_channel"
            val layout = RemoteViews(packageName, R.layout.notification_layout)

            layout.setTextViewText(R.id.notification_title, title)
            layout.setTextViewText(R.id.notification_time, "Entry at $time")
            layout.setTextViewText(R.id.notification_date, "On $date")

            if (imageUrl.isNullOrEmpty()) {
                Log.w("DoorMonitorService", "Image URL is null or empty, using fallback image")
                layout.setImageViewResource(R.id.notification_image, R.drawable.ic_notification)
                showBuiltNotification(notificationManager, layout, channelId)
                return
            }

            Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        try {
                            layout.setImageViewBitmap(R.id.notification_image, resource)
                            showBuiltNotification(notificationManager, layout, channelId)
                        } catch (e: Exception) {
                            Log.e("DoorMonitorService", "Error setting bitmap in notification: ${e.message}")
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        Log.w("DoorMonitorService", "Glide cleared or failed, using placeholder image")
                        layout.setImageViewResource(R.id.notification_image, R.drawable.ic_notification)
                        showBuiltNotification(notificationManager, layout, channelId)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        Log.e("DoorMonitorService", "Glide failed to load image: $imageUrl")
                        layout.setImageViewResource(R.id.notification_image, R.drawable.ic_notification)
                        showBuiltNotification(notificationManager, layout, channelId)
                    }
                })
        } catch (e: Exception) {
            Log.e("DoorMonitorService", "Error in sendNotification: ${e.message}")
        }
    }

    private fun showBuiltNotification(manager: NotificationManager, layout: RemoteViews, channelId: String) {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("targetFragment", "history")  // Pass the extra to navigate to HistoryFragment
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(layout)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)  // Make the notification pressable
                .build()

            val notifId = System.currentTimeMillis().toInt()
            manager.notify(notifId, notification)
            Log.d("DoorMonitorService", "Notification sent with ID: $notifId")
        } catch (e: Exception) {
            Log.e("DoorMonitorService", "Failed to display notification: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    "door_activity_channel",
                    "Door Activity Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for door activity events"
                }
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                Log.d("DoorMonitorService", "Notification channel created")
            } catch (e: Exception) {
                Log.e("DoorMonitorService", "Failed to create notification channel: ${e.message}")
            }
        }
    }

    private fun saveNotifiedKeys() {
        try {
            val prefs = getSharedPreferences("smartdoor_prefs", Context.MODE_PRIVATE)
            val jsonArray = JSONArray()
            notifiedKeys.forEach { jsonArray.put(it) }
            prefs.edit().putString("notified_keys", jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e("DoorMonitorService", "Failed to save notified keys: ${e.message}")
        }
    }

    private fun loadNotifiedKeys() {
        try {
            val prefs = getSharedPreferences("smartdoor_prefs", Context.MODE_PRIVATE)
            val jsonString = prefs.getString("notified_keys", null) ?: return
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                notifiedKeys.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            Log.e("DoorMonitorService", "Failed to load notified keys: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
