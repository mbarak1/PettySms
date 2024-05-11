package com.example.pettysms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.core.app.NotificationCompat


class MySmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
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
                                        val smsDate  = cursor.getString(2).toLong()


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
        val notificationId = System.currentTimeMillis().toInt() // Unique notification ID

        val NOTIFICATION_SOUND_URI = Uri.parse("android.resource://${BuildConfig.APPLICATION_ID}/${R.raw.petty_sms_marimba}")

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.p_logo_cropped)  // Set the transparent small icon
            .setContentTitle("MPESA Notification")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(NOTIFICATION_SOUND_URI)
            .setSilent(true)


        // Create an explicit intent for launching the app when the notification is clicked
        val resultIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationBuilder.setContentIntent(pendingIntent)

        // Show the notification
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Check if the device is running Android Oreo or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel 1",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Notify after MediaPlayer setup
        val mp: MediaPlayer = MediaPlayer.create(context, R.raw.petty_sms_marimba)
        mp.setVolume(1.0f, 1.0f) // Set the volume to 80% of maximum (range: 0.0 to 1.0)
        mp.setOnCompletionListener { mp.release() } // Release the MediaPlayer when sound playback is complete
        mp.setOnPreparedListener { _ ->
            mp.start() // Start playing the sound after MediaPlayer is prepared
            notificationManager.notify(notificationId, notificationBuilder.build())
        }


        // Notify
        notificationManager.notify(0, notificationBuilder.build())
    }
}

