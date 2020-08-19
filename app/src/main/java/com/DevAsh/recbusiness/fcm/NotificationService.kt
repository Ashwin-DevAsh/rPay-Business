package com.DevAsh.recbusiness.fcm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.DevAsh.recbusiness.Context.DetailsContext
import com.DevAsh.recbusiness.Context.HelperVariables
import com.DevAsh.recbusiness.Context.UiContext
import com.DevAsh.recbusiness.Database.Credentials
import com.DevAsh.recbusiness.Database.RealmHelper
import com.DevAsh.recbusiness.Helper.TransactionsHelper
import com.DevAsh.recbusiness.Home.Transactions.SingleObjectTransaction
import com.DevAsh.recbusiness.Models.Contacts
import com.DevAsh.recbusiness.R
import com.DevAsh.recbusiness.SplashScreen
import com.DevAsh.recbusiness.Sync.SocketHelper
import com.DevAsh.recbusiness.Sync.SocketHelper.socketIntent
import com.DevAsh.recbusiness.Sync.SocketService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import io.realm.Realm
import kotlin.random.Random


class NotificationService : FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        println("new token $p0")
        super.onNewToken(p0)
    }

    override fun onCreate() {
        RealmHelper.init(context = this)
        socketIntent = Intent(this,SocketService::class.java)
        super.onCreate()
    }



    override fun onMessageReceived(p0: RemoteMessage) {

        if (p0.data["type"]=="awake"){
            stopService(socketIntent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 startForegroundService(socketIntent)
            } else {
                 startService(socketIntent)
            }
        }else if(p0.data["type"]?.startsWith("updateProfilePicture")!!){
            try{
                val id = p0.data["type"]?.split(",")!![1]
                Picasso.get().invalidate(UiContext.buildURL(id))
                Picasso.get().load(UiContext.buildURL(id))
                    .memoryPolicy(MemoryPolicy.NO_CACHE)
                    .networkPolicy(NetworkPolicy.NO_CACHE)
            }catch (e:Throwable){
                println(e)
            }

        }else if(p0.data["type"]?.startsWith("merchantStatus")!!){

            val id = p0.data["type"]!!.split(",")[1]
            val status = p0.data["type"]!!.split(",")[2]

            val details = Realm.getDefaultInstance().where(Credentials::class.java).findFirst()
            if("rbusiness@${details?.phoneNumber}"==id){
                val intent = Intent(applicationContext,SplashScreen::class.java)
                if(status=="active"){
                     showNotification("R Admin","Your account is successfully activated!",intent)
                     DetailsContext.isVerified=true
                 }else{
                     showNotification("R Admin","Your account is deactivated!",intent)
                     DetailsContext.isVerified=false
                 }
            }

        }else if(p0.data["type"]?.startsWith("message")!!){
            val amount = p0.data["type"]!!.split(",")[3]
            val fromName = p0.data["type"]!!.split(",")[1]
            val fromID =  p0.data["type"]!!.split(",")[2]
            val fromEmail = p0.data["type"]!!.split(",")[4]

            TransactionsHelper.notificationObserver[fromID]?.check()

            HelperVariables.selectedUser = Contacts(fromName,"+"+fromID.split("@")[1],fromID,fromEmail)
            val intent = Intent(applicationContext, SingleObjectTransaction::class.java)
            intent.putExtra("openSingleObjectTransactions",true)
            showNotification(fromName, amount,intent)
        }else{
            HelperVariables.openTransactionPage = true
            val type =  p0.data["type"]!!.split(",")[0]
            val amount = p0.data["type"]!!.split(",")[3]
            val fromName = p0.data["type"]!!.split(",")[1]
            val fromID =  p0.data["type"]!!.split(",")[2]
            val fromEmail = p0.data["type"]!!.split(",")[4]

            try {
                SocketHelper.getMyState()
            } catch (e:Throwable){

            }

            HelperVariables.selectedUser = Contacts(fromName,"+"+fromID.split("@")[1],fromID,fromEmail)

            when (type) {
                "addedMoney" -> {
                    val intent = Intent(applicationContext,SplashScreen::class.java)
                    intent.putExtra("openSingleObjectTransactions",true)
                    showNotification(
                        "Added Money",
                        "Your $amount ${HelperVariables.currency}s has been successfully added.",intent)
                }
                "withdraw" -> {
                    val intent = Intent(applicationContext,SplashScreen::class.java)
                    intent.putExtra("openSingleObjectTransactions",true)
                    showNotification("withdraw","Your $amount ${HelperVariables.currency}s has been successfully withdraw.",intent)
                }
                else -> {
                    TransactionsHelper.notificationObserver[fromID]?.check()
                    val intent = Intent(applicationContext,SingleObjectTransaction::class.java)
                    intent.putExtra("openSingleObjectTransactions",true)
                    showNotification(fromName,"You have received $amount ${HelperVariables.currency}s from $fromName.",intent)
                }
            }


        }
        super.onMessageReceived(p0)
    }

    private fun showNotification(title:String, subTitle:String,intent:Intent){
        var notificationID = Random.nextInt(1000000000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "Payment"
            val channelName: CharSequence = "Payment"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(notificationChannel)

            val builder: Notification.Builder = Notification.Builder(this, "Payment")
                .setContentTitle(title)
                .setContentText(subTitle)
                .setSmallIcon(R.drawable.rpay_notification)
                .setContentIntent(
                    PendingIntent.getActivities(this, 1, arrayOf(intent), PendingIntent.FLAG_UPDATE_CURRENT)
                )
                .setAutoCancel(true)
            val notification: Notification = builder.build()
            notification.flags = Notification.FLAG_AUTO_CANCEL
            notificationManager.notify(notificationID,notification)
        } else {
            val builder = NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(subTitle)
                .setSmallIcon(R.drawable.rpay_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(
                    PendingIntent.getActivities(this, 1, arrayOf(intent), PendingIntent.FLAG_UPDATE_CURRENT)
                )
                .setAutoCancel(true)
            val notification: Notification = builder.build()
            notification.flags = Notification.FLAG_AUTO_CANCEL
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(Random.nextInt(1000000000),notification)
        }
    }

}