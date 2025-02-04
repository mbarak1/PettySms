package com.example.pettysms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat


class MySmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("MySmsReceiver", "onReceive() called")
        if (intent?.action == "android.provider.Telephony.SMS_RECEIVED") {
            // Handle the incoming SMS message here
            // Extract the message details from the intent
            val bundle = intent.extras
            var isServiceScheduled = false
            val pdus = bundle?.get("pdus") as Array<*>?

            // Check if context is null
            if (context == null) {
                return
            }

            var smsServiceHelper = SmsServiceHelper()

            //store running state of the foreground service
            val prefs = context.getSharedPreferences("YourPrefsName", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putBoolean("isForegroundServiceRunning", true)
            editor.apply()

            // Process the SMS messages
            pdus?.let { pdus ->
                for (pdu in pdus) {
                    val message = SmsMessage.createFromPdu(pdu as ByteArray)
                    val sender = message.originatingAddress
                    val body = message.messageBody
                    val id = message.indexOnIcc
                    val timestamp = message.timestampMillis
                    val cr: ContentResolver = context?.contentResolver!!
                    val cursor = cr.query(
                        Telephony.Sms.Inbox.CONTENT_URI,
                        arrayOf(
                            Telephony.Sms.Inbox._ID,
                            Telephony.Sms.Inbox.BODY,
                            Telephony.Sms.Inbox.DATE
                        ),
                        null,
                        null,
                        null
                    )
                    val sms = java.util.ArrayList<MutableList<String>>()
                    val lstSms: MutableList<String> = java.util.ArrayList()
                    val lstRcvr: MutableList<String> = java.util.ArrayList()
                    var lstDate: MutableList<String> = java.util.ArrayList()
                    var lstId: MutableList<String> = java.util.ArrayList()


                    // Check if the SMS is from the desired sender (e.g., "MPESA")
                    if (sender == "MPESA") {
                        // Trigger a notification or perform any desired action
                        if (context != null) {
                            println("sasa hiyo id: " + id.toString())

                            if (cursor != null) {
                                try {
                                    while (cursor.moveToNext()) {
                                        val messageId = cursor.getString(0)
                                        val smsBody = cursor.getString(1)
                                        val smsDate = cursor.getString(2).toLong()


                                        // Check if the content of the SMS matches what you're looking for
                                        if (smsBody.contains(body)) {
                                            lstSms.add(smsBody)
                                            lstDate.add(smsDate.toString())
                                            lstRcvr.add(sender)
                                            lstId.add(messageId)
                                            sms.add(lstSms)
                                            sms.add(lstRcvr)
                                            sms.add(lstDate)
                                            sms.add(lstId)
                                            // Process the SMS
                                            smsServiceHelper.processSms(sms, context)
                                            println("smsDate " + smsDate)
                                            println("messagetimestamp " + timestamp)
                                            println("Message ID in Inbox: $messageId")
                                            break

                                        }
                                        break
                                    }
                                } finally {
                                    cursor.close()
                                }
                            }
                            showNotification(context, body)
                            editor.putBoolean("isForegroundServiceRunning", false)
                            editor.apply()
                            break
                        }
                    }
                }
            }
        }
    }

    private fun showNotification(context: Context, message: String) {
        val channelId = "channel_1"
        val notificationId = System.currentTimeMillis().toInt()

        // Define the sound URI and notification sound resource
        val notificationSoundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.petty_sms_marimba}")

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.p_logo_cropped)
            .setContentTitle("MPESA Notification")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(notificationSoundUri)

        // Define an explicit intent for MainActivity when clicked
        val resultIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationBuilder.setContentIntent(pendingIntent)

        // Retrieve NotificationManager and configure the channel
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Define and create notification channel for Android 8+ (Oreo and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Channel 1", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for MPESA transactions"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Android 13+ (Tiramisu and above): Ensure POST_NOTIFICATIONS permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(notificationId, notificationBuilder.build())
                Log.d("Notification", "Notification displayed successfully.")
            } else {
                Log.d("Notification", "POST_NOTIFICATIONS permission is not granted.")
            }
        } else {
            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.d("Notification", "Notification displayed for pre-Android 13 device.")
        }

        // Handle sound for pre-Android 14 devices
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val mediaPlayer = MediaPlayer.create(context, R.raw.petty_sms_marimba)
            mediaPlayer.setVolume(1.0f, 1.0f)
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.setOnPreparedListener { it.start() }
        }
    }

}

