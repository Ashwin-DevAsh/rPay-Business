package com.DevAsh.recbusiness.Sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Database.Credentials
import com.DevAsh.recbusiness.Database.RealmHelper
import com.DevAsh.recbusiness.R
import com.androidnetworking.AndroidNetworking
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.jacksonandroidnetworking.JacksonParserFactory
import io.realm.Realm


class SocketService : Service() {

    val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {


        if (DetailsContext.name == null) {
            RealmHelper.init(this)
            AndroidNetworking.initialize(applicationContext)
            AndroidNetworking.setParserFactory(JacksonParserFactory())
            val credentials =
                Realm.getDefaultInstance().where(Credentials::class.java).findFirst() ?: return
            DetailsContext.setData(
                credentials!!.name,
                credentials.phoneNumber,
                credentials.email,
                credentials.password,
                credentials.token,
                credentials.status,
                credentials.storeName
            )

        }
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    println("Failed . . . .")
                    return@OnCompleteListener
                } else {
                    println("success . . . .")
                    DetailsContext.fcmToken = task.result?.token!!
                    SocketHelper.connect()
                }
            })

        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        handler.postDelayed({
//            println("Try to destroy")
//            onDestroy()
//        },1*60*1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channelId = "DevAsh"
            val channelName: CharSequence = "Mining"
            val importance = NotificationManager.IMPORTANCE_NONE
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(notificationChannel)
            val builder: Notification.Builder = Notification.Builder(this, "DevAsh")
                .setContentTitle("Verifying transactions")
                .setContentText("Your device is a node in our blockchain network")
                .setSmallIcon(R.drawable.rpay_notification)
                .setAutoCancel(true)
            val notification: Notification = builder.build()
            startForeground(1, notification)
        } else {
            val builder = NotificationCompat.Builder(this)
                .setContentTitle("Verifying transactions")
                .setContentText("Your device is a node in our blockchain network")
                .setSmallIcon(R.drawable.rpay_notification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
            val notification: Notification = builder.build()
            startForeground(1, notification)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        println("destroying ....")
        super.onDestroy()
    }
}
